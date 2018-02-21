package com.amarkhel.tournament.impl

import java.time.LocalDateTime

import com.amarkhel.mafia.utils.JsonFormats.singletonFormat
import com.amarkhel.tournament.api._
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

class TournamentEntity extends PersistentEntity {
  override type Command = TournamentCommand
  override type Event = TournamentEvent
  override type State = Option[Tournament]
  override def initialState = None

  override def behavior: Behavior = {
    case None => {
      Actions().onCommand[CreateTournament, Tournament] {
        case (CreateTournament(tournament), ctx, _) =>
          ctx.thenPersist(TournamentCreated(tournament))(_ => ctx.reply(tournament))
      }.onReadOnlyCommand[DeleteTournament.type, Boolean] {
        case (DeleteTournament, ctx, _) => ctx.invalidCommand("Tournament not exists")
      }.onReadOnlyCommand[StartGame, Boolean] {
        case (StartGame(_, _), ctx, _) => ctx.invalidCommand("Tournament not exists")
      }.onReadOnlyCommand[ExpireGame, Boolean] {
        case (ExpireGame(_), ctx, _) => ctx.invalidCommand("Tournament not exists")
      }.onReadOnlyCommand[NextRound, Boolean] {
        case (NextRound(_), ctx, _) => ctx.invalidCommand("Tournament not exists")
      }.onReadOnlyCommand[Choose, Boolean] {
        case (Choose(_, _), ctx, _) => ctx.invalidCommand("Tournament not exists")
      }.onReadOnlyCommand[GetTournament.type, Option[Tournament]] {
        case (GetTournament, ctx, _) => ctx.reply(None)
      }.onReadOnlyCommand[StartTournament.type, Boolean] {
        case (StartTournament, ctx, _) => ctx.invalidCommand("Tournament not exists")
      }.onReadOnlyCommand[FinishTournament.type, Boolean] {
        case (FinishTournament, ctx, _) => ctx.invalidCommand("Tournament not exists")
      }.onReadOnlyCommand[JoinTournament, Boolean] {
        case (JoinTournament(_), ctx, _) => ctx.invalidCommand("Tournament not exists")
      }.onEvent {
        case (TournamentCreated(tournament), _) => Some(tournament)
      }
    }
    case Some(t) if (t.finished) => {
      Actions().onReadOnlyCommand[DeleteTournament.type, Boolean] {
        case (DeleteTournament, ctx, _) => ctx.invalidCommand("Tournament already finished and cannot be deleted")
      }.onReadOnlyCommand[GetTournament.type, Option[Tournament]] {
        case (GetTournament, ctx, _) => ctx.reply(Some(t))
      }.onReadOnlyCommand[FinishTournament.type, Boolean] {
        case (FinishTournament, ctx, _) => ctx.invalidCommand("Tournament already finished")
      }.onReadOnlyCommand[StartTournament.type, Boolean] {
        case (StartTournament, ctx, _) => ctx.invalidCommand("Tournament already finished")
      }.onReadOnlyCommand[CreateTournament, Tournament] {
        case (CreateTournament(_), ctx, _) => ctx.invalidCommand("Tournament already finished")
      }.onReadOnlyCommand[StartGame, Boolean] {
        case (StartGame(_, _), ctx, _) => ctx.invalidCommand("Tournament already finished")
      }.onReadOnlyCommand[NextRound, Boolean] {
        case (NextRound(_), ctx, _) => ctx.invalidCommand("Tournament already finished")
      }.onReadOnlyCommand[ExpireGame, Boolean] {
        case (ExpireGame(_), ctx, _) => ctx.invalidCommand("Tournament already finished")
      }.onReadOnlyCommand[Choose, Boolean] {
        case (Choose(_, _), ctx, _) => ctx.invalidCommand("Tournament already finished")
      }.onReadOnlyCommand[JoinTournament, Boolean] {
        case (JoinTournament(_), ctx, _) => ctx.invalidCommand("Tournament already finished")
      }
    }
    case Some(t) => {
      Actions().onCommand[DeleteTournament.type, Boolean] {
        case (DeleteTournament, ctx, _) =>
          ctx.thenPersist(TournamentDeleted)(_ => ctx.reply(true))
      }.onReadOnlyCommand[GetTournament.type, Option[Tournament]] {
        case (GetTournament, ctx, _) => ctx.reply(Some(t))
      }.onCommand[FinishTournament.type, Boolean] {
        case (FinishTournament, ctx, tournament) => {
          val players =  for {
            pl <- tournament.get.players
            games = for {
              game <- pl.games
              modified = game.expire
            }
            yield(modified)
            p = pl.copy(games = games)
          } yield p
          val t = tournament.get.copy(players = players, finish = Some(LocalDateTime.now()))
          ctx.thenPersist(TournamentFinished(t))(_ => ctx.reply(true))
        }
      }.onCommand[StartTournament.type, Boolean] {
        case (StartTournament, ctx, tournament) => {
          val t = tournament.get.copy(start = Some(LocalDateTime.now()))
          ctx.thenPersist(TournamentStarted(t))(_ => ctx.reply(true))
        }
      }.onCommand[StartGame, Boolean] {
        case (StartGame(user, id), ctx, tournament) => {
          val result = for {
            t <- tournament
            player <- t.findPlayer(user)
            game <- player.findGame(id) if(game.notStarted)
            g = game.copy(started = Some(LocalDateTime.now()))
            games = player.games.updated(player.games.indexOf(game), g)
            p = player.copy(games = games)
            players = t.players.updated(t.players.indexOf(player), p)
            tour = t.copy(players = players)
          } yield tour
          if(result.isDefined){
            ctx.thenPersist(GameStarted(result.get))(_ => ctx.reply(true))
          } else {
            ctx.invalidCommand("Something wrong happened")
            ctx.done
          }
        }
      }.onCommand[Choose, Boolean] {
        case (Choose(user, pl), ctx, tournament) => {
          val result = for {
            t <- tournament
            player <- t.findPlayer(user)
            game <- player.findStartedGame
            modified = game.choose(pl)
            games = player.games.updated(player.games.indexOf(game), modified)
            p = player.copy(games = games)
            players = t.players.updated(t.players.indexOf(player), p)
            tour = t.copy(players = players)
          } yield tour
          if(result.isDefined){
            ctx.thenPersist(GameStarted(result.get))(_ => ctx.reply(true))
          } else {
            ctx.invalidCommand("Something wrong happened")
            ctx.done
          }
        }
      }.onCommand[NextRound, Boolean] {
        case (NextRound(user), ctx, tournament) => {
          val result = for {
            t <- tournament
            player <- t.findPlayer(user)
            game <- player.findStartedGame
            modified = game.nextRound
            games = player.games.updated(player.games.indexOf(game), modified)
            p = player.copy(games = games)
            players = t.players.updated(t.players.indexOf(player), p)
            tour = t.copy(players = players)
          } yield tour
          if(result.isDefined){
            ctx.thenPersist(NextRoundStarted(result.get))(_ => ctx.reply(true))
          } else {
            ctx.invalidCommand("Something wrong happened")
            ctx.done
          }
        }
      }.onCommand[ExpireGame, Boolean] {
        case (ExpireGame(user), ctx, tournament) => {
          val result = for {
            t <- tournament
            player <- t.findPlayer(user)
            game <- player.findStartedGame
            modified = game.expire
            games = player.games.updated(player.games.indexOf(game), modified)
            p = player.copy(games = games)
            players = t.players.updated(t.players.indexOf(player), p)
            tour = t.copy(players = players)
          } yield tour
          if(result.isDefined){
            ctx.thenPersist(NextRoundStarted(result.get))(_ => ctx.reply(true))
          } else {
            ctx.invalidCommand("Something wrong happened")
            ctx.done
          }
        }
      }.onCommand[JoinTournament, Boolean] {
        case (JoinTournament(user), ctx, tournament) => {
          if(tournament.get.countJoinedPlayers == tournament.get.countPlayers) {
            ctx.invalidCommand("Все люди уже набраны на этот турнир")
            ctx.done
          } else {
            val results = for {
              descr <- tournament.get.games
              solution = Solution(Map[String, Int](), SolutionStatus.AWAITING)
              result = GameResult(descr.id, 0, descr, solution, None)
            } yield result
            val userState = UserState(user, results)
            val modified = t.copy(players = tournament.get.players :+ userState)
            ctx.thenPersist(Joined(modified))(_ => ctx.reply(true))
          }
        }
      }.onReadOnlyCommand[CreateTournament, Tournament] {
        case (CreateTournament(_), ctx, _) => ctx.invalidCommand("Tournament already exists")
      }.onEvent {
        case (TournamentDeleted, _) => None
        case (TournamentFinished(tournament), _) => Some(tournament)
        case (GameStarted(tournament), _) => Some(tournament)
        case (NextRoundStarted(tournament), _) => Some(tournament)
        case (TournamentStarted(tournament), _) => Some(tournament)
      }
    }
  }
}

sealed trait TournamentEvent

case class TournamentCreated(tournament:Tournament) extends TournamentEvent
case class TournamentFinished(tournament:Tournament) extends TournamentEvent
case class TournamentStarted(tournament:Tournament) extends TournamentEvent
case class GameStarted(tournament:Tournament) extends TournamentEvent
case class GameExpired(tournament:Tournament) extends TournamentEvent
case class Joined(tournament:Tournament) extends TournamentEvent
case class Chosen(tournament:Tournament) extends TournamentEvent
case class NextRoundStarted(tournament:Tournament) extends TournamentEvent
case object TournamentDeleted extends TournamentEvent{
  implicit val format: Format[TournamentDeleted.type] = singletonFormat(TournamentDeleted)
}

object TournamentCreated {
  implicit val format: Format[TournamentCreated] = Json.format
}

object TournamentFinished {
  implicit val format: Format[TournamentFinished] = Json.format
}

object TournamentStarted {
  implicit val format: Format[TournamentStarted] = Json.format
}

object Chosen {
  implicit val format: Format[Chosen] = Json.format
}

object GameStarted {
  implicit val format: Format[GameStarted] = Json.format
}

object GameExpired {
  implicit val format: Format[GameExpired] = Json.format
}

object Joined {
  implicit val format: Format[Joined] = Json.format
}

object NextRoundStarted {
  implicit val format: Format[NextRoundStarted] = Json.format
}

sealed trait TournamentCommand

case class CreateTournament(tournament:Tournament) extends TournamentCommand with ReplyType[Tournament]

case class StartGame(user:String, id:Int) extends TournamentCommand with ReplyType[Boolean]

case class NextRound(user:String) extends TournamentCommand with ReplyType[Boolean]

case class Choose(user:String, player:String) extends TournamentCommand with ReplyType[Boolean]

case class ExpireGame(user:String) extends TournamentCommand with ReplyType[Boolean]

case class JoinTournament(user:String) extends TournamentCommand with ReplyType[Boolean]

case object FinishTournament extends TournamentCommand with ReplyType[Boolean]{
  implicit val format: Format[FinishTournament.type] = singletonFormat(FinishTournament)
}

case object DeleteTournament extends TournamentCommand with ReplyType[Boolean]{
  implicit val format: Format[DeleteTournament.type] = singletonFormat(DeleteTournament)
}

case object GetTournament extends TournamentCommand with ReplyType[Option[Tournament]]{
  implicit val format: Format[GetTournament.type] = singletonFormat(GetTournament)
}

case object StartTournament extends TournamentCommand with ReplyType[Boolean]{
  implicit val format: Format[StartTournament.type] = singletonFormat(StartTournament)
}

object CreateTournament {
  implicit val format: Format[CreateTournament] = Json.format
}

object Choose {
  implicit val format: Format[Choose] = Json.format
}

object StartGame {
  implicit val format: Format[StartGame] = Json.format
}

object ExpireGame {
  implicit val format: Format[ExpireGame] = Json.format
}

object NextRound {
  implicit val format: Format[NextRound] = Json.format
}

object JoinTournament {
  implicit val format: Format[JoinTournament] = Json.format
}

object TournamentSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[DeleteTournament.type],
    JsonSerializer[FinishTournament.type],
    JsonSerializer[CreateTournament],
    JsonSerializer[StartTournament.type],
    JsonSerializer[JoinTournament],
    JsonSerializer[StartGame],
    JsonSerializer[ExpireGame],
    JsonSerializer[Choose],
    JsonSerializer[NextRound],
    JsonSerializer[Chosen],
    JsonSerializer[NextRoundStarted],
    JsonSerializer[GameExpired],
    JsonSerializer[GameStarted],
    JsonSerializer[GetTournament.type],
    JsonSerializer[TournamentDeleted.type],
    JsonSerializer[TournamentFinished],
    JsonSerializer[TournamentCreated],
    JsonSerializer[Joined],
    JsonSerializer[TournamentStarted]
  )
}