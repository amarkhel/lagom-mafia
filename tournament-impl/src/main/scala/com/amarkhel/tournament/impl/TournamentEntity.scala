package com.amarkhel.tournament.impl

import com.amarkhel.mafia.utils.JsonFormats.singletonFormat
import com.amarkhel.tournament.api._
import com.amarkhel.tournament.impl.TournamentEntity._
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

object TournamentEntity {
  val TOURNAMENT_NOT_FOUND = "Турнира с таким именем не существует"
  val TOURNAMENT_FINISHED = "Турнир уже закончился"
  val TOURNAMENT_NOT_STARTED = "Турнир еще не начался"
  val SOMETHING_WRONG = "Произошло что-то непредвиденное, попробуйте зайти позже"
  val TOURNAMENT_ALREADY_STARTED = "Турнир уже начался"
  val ALL_PEOPLE_JOINED = "Все люди уже набраны на этот турнир"
  val ALREADY_JOINED = "Вы уже добавлены к этому турниру"
  val GAME_IN_PROGRESS = "Текущая игра уже выбрана"
  val NO_CURRENT_GAME = "Нету текущей игры"
  val BLANK_NAME = "Невозможно добавить пользователя без имени"
  val GAME_NOT_FOUND = "Игра с таким айди не найдена"
  val GAME_WAS_STARTED = "Эта игра уже стартовала"
  val ANOTHER_GAME_IN_PROGRESS = "Нельзя остановить игру, пока другая находится в прогрессе"
  val PLAYER_NOT_JOINED_TO_TOURNAMENT = "Вы не участник турнира"
  val ALREADY_VOTED_FOR_THIS_PLAYER = "Вы уже проголосовали за этого игрока"
  val ALL_MAFIA_CHOSEN = "Вы уже выбрали всех мафиози"
  val LAST_ROUND_ALREADY_CHOSEN = "Последний раунд уже выбран"
}

class TournamentEntity extends PersistentEntity {
  override type Command = TournamentCommand
  override type Event = TournamentEvent
  override type State = Option[Tournament]
  override def initialState = None

  val notExistBehavior = Actions().onCommand[CreateTournament, Tournament] {
    case (CreateTournament(tournament), ctx, _) => ctx.thenPersist(TournamentCreated(tournament))(_ => ctx.reply(tournament))
  }.onReadOnlyCommand[GetTournament.type, Option[Tournament]] {
    case (GetTournament, ctx, _) => ctx.reply(None)
  }.onReadOnlyCommand[DeleteTournament.type, Boolean] {
    case (DeleteTournament, ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_FOUND)
  }.onReadOnlyCommand[UpdateTournament, Tournament] {
    case (UpdateTournament(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_FOUND)
  }.onReadOnlyCommand[StartGame, Boolean] {
    case (StartGame(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_FOUND)
  }.onReadOnlyCommand[FinishGame, Boolean] {
    case (FinishGame(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_FOUND)
  }.onReadOnlyCommand[NextRound, Boolean] {
    case (NextRound(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_FOUND)
  }.onReadOnlyCommand[Choose, Boolean] {
    case (Choose(_, _), ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_FOUND)
  }.onReadOnlyCommand[StartTournament.type, Boolean] {
    case (StartTournament, ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_FOUND)
  }.onReadOnlyCommand[FinishTournament.type, Boolean] {
    case (FinishTournament, ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_FOUND)
  }.onReadOnlyCommand[JoinTournament, Boolean] {
    case (JoinTournament(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_FOUND)
  }.onEvent {
    case (TournamentCreated(tournament), _) => Some(tournament)
  }

  val notStartedBehavior = Actions().onCommand[DeleteTournament.type, Boolean] {
    case (DeleteTournament, ctx, _) => ctx.thenPersist(TournamentDeleted)(_ => ctx.reply(true))
  }.onCommand[UpdateTournament, Tournament] {
    case (UpdateTournament(tournament), ctx, _) => ctx.thenPersist(TournamentUpdated(tournament))(_ => ctx.reply(tournament))
  }.onReadOnlyCommand[GetTournament.type, Option[Tournament]] {
    case (GetTournament, ctx, t) => ctx.reply(Some(t.get))
  }.onReadOnlyCommand[FinishTournament.type, Boolean] {
    case (FinishTournament, ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_STARTED)
  }.onCommand[StartTournament.type, Boolean] {
    case (StartTournament, ctx, _) => ctx.thenPersist(TournamentStarted)(_ => ctx.reply(true))
  }.onReadOnlyCommand[CreateTournament, Tournament] {
    case (CreateTournament(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_ALREADY_STARTED)
  }.onReadOnlyCommand[StartGame, Boolean] {
    case (StartGame(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_STARTED)
  }.onReadOnlyCommand[NextRound, Boolean] {
    case (NextRound(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_STARTED)
  }.onReadOnlyCommand[FinishGame, Boolean] {
    case (FinishGame(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_STARTED)
  }.onReadOnlyCommand[Choose, Boolean] {
    case (Choose(_, _), ctx, _) => ctx.invalidCommand(TOURNAMENT_NOT_STARTED)
  }.onCommand[JoinTournament, Boolean] {
    case (JoinTournament(user), ctx, tournament) => {
      val t = tournament.get
      if(user == null || user.isEmpty) {
        ctx.invalidCommand(BLANK_NAME)
        ctx.done
      } else if (t.allPlayersJoined) {
        ctx.invalidCommand(ALL_PEOPLE_JOINED)
        ctx.done
      } else if (t.havePlayer(user)) {
        ctx.invalidCommand(ALREADY_JOINED)
        ctx.done
      } else {
        ctx.thenPersist(Joined(user))(_ => ctx.reply(true))
      }
    }
  }.onEvent {
    case (TournamentDeleted, _) => None
    case (Joined(name), t) => Some(t.get.join(name))
    case (TournamentStarted, t) => Some(t.get.startTournament)
    case (TournamentUpdated(tournament), _) => Some(tournament)
  }

  val gameInProgressBehavior = Actions().onReadOnlyCommand[DeleteTournament.type, Boolean] {
    case (DeleteTournament, ctx, _) => ctx.invalidCommand(TOURNAMENT_ALREADY_STARTED)
  }.onReadOnlyCommand[GetTournament.type, Option[Tournament]] {
    case (GetTournament, ctx, t) => ctx.reply(Some(t.get))
  }.onReadOnlyCommand[FinishTournament.type, Boolean] {
    case (FinishTournament, ctx, _) => ctx.invalidCommand(TOURNAMENT_ALREADY_STARTED)
  }.onReadOnlyCommand[StartTournament.type, Boolean] {
    case (StartTournament, ctx, _) => ctx.invalidCommand(TOURNAMENT_ALREADY_STARTED)
  }.onReadOnlyCommand[StartGame, Boolean] {
    case (StartGame(_), ctx, _) => ctx.invalidCommand(GAME_IN_PROGRESS)
  }.onCommand[Choose, Boolean] {
    case (Choose(user, pl), ctx, tournament) => {
      val t = tournament.get
      if(!t.havePlayer(user)){
        ctx.invalidCommand(PLAYER_NOT_JOINED_TO_TOURNAMENT)
        ctx.done
      } else if(t.alreadyVoted(user, pl)){
        ctx.invalidCommand(ALREADY_VOTED_FOR_THIS_PLAYER)
        ctx.done
      } else if(t.isGameFinishedForUser(user)){
        ctx.invalidCommand(ALL_MAFIA_CHOSEN)
        ctx.done
      } else ctx.thenPersist(Chosen(user, pl, t.inProgressGameId))(_ => ctx.reply(true))
    }
  }.onCommand[NextRound, Boolean] {
    case (NextRound(user), ctx, tournament) => {
      val t = tournament.get
      if(!t.havePlayer(user)){
        ctx.invalidCommand(PLAYER_NOT_JOINED_TO_TOURNAMENT)
        ctx.done
      } else if(t.isGameFinishedForUser(user)) {
        ctx.invalidCommand(LAST_ROUND_ALREADY_CHOSEN)
        ctx.done
      }
      else ctx.thenPersist(NextRoundStarted(user, t.inProgressGameId))(_ => ctx.reply(true))
    }
  }.onCommand[FinishGame, Boolean] {
    case (FinishGame(id), ctx, t) => {
      if(t.get.inProgressGameId != id) {
        ctx.invalidCommand(ANOTHER_GAME_IN_PROGRESS)
        ctx.done
      } else {
        ctx.thenPersist(GameFinished(id))(_ => ctx.reply(true))
      }
    }
  }.onReadOnlyCommand[JoinTournament, Boolean] {
    case (JoinTournament(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_ALREADY_STARTED)
  }.onReadOnlyCommand[CreateTournament, Tournament] {
    case (CreateTournament(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_ALREADY_STARTED)
  }.onReadOnlyCommand[UpdateTournament, Tournament] {
    case (UpdateTournament(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_ALREADY_STARTED)
  }.onEvent {
    case (Chosen(pl, maf, id), t) => Some(t.get.chooseMafia(pl, maf, id))
    case (NextRoundStarted(pl, id), t) => Some(t.get.nextRound(pl, id))
    case (GameFinished(id), t) => Some(t.get.finishGame(id))
    case (TournamentFinished, t) => Some(t.get.finishTournament)
  }

  val betweenGamesBehavior = Actions().onReadOnlyCommand[DeleteTournament.type, Boolean] {
    case (DeleteTournament, ctx, _) => ctx.invalidCommand(TOURNAMENT_ALREADY_STARTED)
  }.onReadOnlyCommand[GetTournament.type, Option[Tournament]] {
    case (GetTournament, ctx, t) => ctx.reply(Some(t.get))
  }.onCommand[FinishTournament.type, Boolean] {
    case (FinishTournament, ctx, _) => ctx.thenPersist(TournamentFinished)(_ => ctx.reply(true))
  }.onReadOnlyCommand[StartTournament.type, Boolean] {
    case (StartTournament, ctx, _) => ctx.invalidCommand(TOURNAMENT_ALREADY_STARTED)
  }.onCommand[StartGame, Boolean] {
    case (StartGame(id), ctx, t) => {
      if(!t.get.gameExist(id)) {
        ctx.invalidCommand(GAME_NOT_FOUND)
        ctx.done
      } else if (t.get.wasStarted(id)) {
        ctx.invalidCommand(GAME_WAS_STARTED)
        ctx.done
      }
      else ctx.thenPersist(GameStarted(id))(_ => ctx.reply(true))
    }
  }.onReadOnlyCommand[Choose, Boolean] {
    case (Choose(_, _), ctx, _) => ctx.invalidCommand(NO_CURRENT_GAME)
  }.onReadOnlyCommand[NextRound, Boolean] {
    case (NextRound(_), ctx, _) => ctx.invalidCommand(NO_CURRENT_GAME)
  }.onReadOnlyCommand[FinishGame, Boolean] {
    case (FinishGame(_), ctx, _) => ctx.invalidCommand(NO_CURRENT_GAME)
  }.onReadOnlyCommand[JoinTournament, Boolean] {
    case (JoinTournament(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_ALREADY_STARTED)
  }.onReadOnlyCommand[CreateTournament, Tournament] {
    case (CreateTournament(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_ALREADY_STARTED)
  }.onReadOnlyCommand[UpdateTournament, Tournament] {
    case (UpdateTournament(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_ALREADY_STARTED)
  }.onEvent {
    case (TournamentFinished, t) => Some(t.get.finishTournament)
    case (GameStarted(id), t) => Some(t.get.startGame(id))
  }

  val alreadyFinishedBehavior = Actions().onReadOnlyCommand[DeleteTournament.type, Boolean] {
    case (DeleteTournament, ctx, _) => ctx.invalidCommand(TOURNAMENT_FINISHED)
  }.onReadOnlyCommand[GetTournament.type, Option[Tournament]] {
    case (GetTournament, ctx, t) => ctx.reply(Some(t.get))
  }.onReadOnlyCommand[FinishTournament.type, Boolean] {
    case (FinishTournament, ctx, _) => ctx.invalidCommand(TOURNAMENT_FINISHED)
  }.onReadOnlyCommand[StartTournament.type, Boolean] {
    case (StartTournament, ctx, _) => ctx.invalidCommand(TOURNAMENT_FINISHED)
  }.onReadOnlyCommand[CreateTournament, Tournament] {
    case (CreateTournament(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_FINISHED)
  }.onReadOnlyCommand[UpdateTournament, Tournament] {
    case (UpdateTournament(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_FINISHED)
  }.onReadOnlyCommand[StartGame, Boolean] {
    case (StartGame(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_FINISHED)
  }.onReadOnlyCommand[FinishGame, Boolean] {
    case (FinishGame(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_FINISHED)
  }.onReadOnlyCommand[NextRound, Boolean] {
    case (NextRound(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_FINISHED)
  }.onReadOnlyCommand[Choose, Boolean] {
    case (Choose(_, _), ctx, _) => ctx.invalidCommand(TOURNAMENT_FINISHED)
  }.onReadOnlyCommand[JoinTournament, Boolean] {
    case (JoinTournament(_), ctx, _) => ctx.invalidCommand(TOURNAMENT_FINISHED)
  }

  override def behavior: Behavior = {
    case None => notExistBehavior
    case Some(t) if t.finished => alreadyFinishedBehavior
    case Some(t) if !t.started => notStartedBehavior
    case Some(t) if t.hasGameInProgress => gameInProgressBehavior
    case Some(_) => betweenGamesBehavior
  }
}

trait TournamentEvent extends AggregateEvent[TournamentEvent] {
  override def aggregateTag: AggregateEventTagger[TournamentEvent] = TournamentEvent.Tag
}
case object TournamentEvent{
  val Tag = AggregateEventTag.sharded[TournamentEvent](5)
  implicit val formatTC: Format[TournamentCreated] = Json.format
  implicit val formatTU: Format[TournamentUpdated] = Json.format
  implicit val formatTD: Format[TournamentDeleted.type] = singletonFormat(TournamentDeleted)
  implicit val formatTF: Format[TournamentFinished.type] = singletonFormat(TournamentFinished)
  implicit val formatTS: Format[TournamentStarted.type] = singletonFormat(TournamentStarted)
  implicit val formatCH: Format[Chosen] = Json.format
  implicit val formatGS: Format[GameStarted] = Json.format
  implicit val formatGF: Format[GameFinished] = Json.format
  implicit val formatJ: Format[Joined] = Json.format
  implicit val formatNR: Format[NextRoundStarted] = Json.format
}

case class TournamentCreated(tournament:Tournament) extends TournamentEvent
case class TournamentUpdated(tournament:Tournament) extends TournamentEvent
case object TournamentFinished extends TournamentEvent
case object TournamentStarted extends TournamentEvent
case object TournamentDeleted extends TournamentEvent
case class GameStarted(id:Int) extends TournamentEvent
case class GameFinished(id:Int) extends TournamentEvent
case class Joined(player:String) extends TournamentEvent
case class Chosen(player:String, target:String, game:Int) extends TournamentEvent
case class NextRoundStarted(user:String, game:Int) extends TournamentEvent

sealed trait TournamentCommand

object TournamentCommand {
  implicit val formatFT: Format[FinishTournament.type] = singletonFormat(FinishTournament)
  implicit val formatDT: Format[DeleteTournament.type] = singletonFormat(DeleteTournament)
  implicit val formatGT: Format[GetTournament.type] = singletonFormat(GetTournament)
  implicit val formatST: Format[StartTournament.type] = singletonFormat(StartTournament)
  implicit val formatUT: Format[UpdateTournament.type] = singletonFormat(UpdateTournament)
  implicit val formatCT: Format[CreateTournament] = Json.format
  implicit val formatCH: Format[Choose] = Json.format
  implicit val formatSG: Format[StartGame] = Json.format
  implicit val formatFG: Format[FinishGame] = Json.format
  implicit val formatNR: Format[NextRound] = Json.format
  implicit val formatJT: Format[JoinTournament] = Json.format
}

case class CreateTournament(tournament:Tournament) extends TournamentCommand with ReplyType[Tournament]
case class UpdateTournament(tournament:Tournament) extends TournamentCommand with ReplyType[Tournament]
case class StartGame(id:Int) extends TournamentCommand with ReplyType[Boolean]
case class NextRound(user:String) extends TournamentCommand with ReplyType[Boolean]
case class Choose(user:String, player:String) extends TournamentCommand with ReplyType[Boolean]
case class FinishGame(id:Int) extends TournamentCommand with ReplyType[Boolean]
case class JoinTournament(user:String) extends TournamentCommand with ReplyType[Boolean]
case object FinishTournament extends TournamentCommand with ReplyType[Boolean]
case object DeleteTournament extends TournamentCommand with ReplyType[Boolean]
case object GetTournament extends TournamentCommand with ReplyType[Option[Tournament]]
case object StartTournament extends TournamentCommand with ReplyType[Boolean]

object TournamentSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[DeleteTournament.type],
    JsonSerializer[FinishTournament.type],
    JsonSerializer[CreateTournament],
    JsonSerializer[StartTournament.type],
    JsonSerializer[JoinTournament],
    JsonSerializer[StartGame],
    JsonSerializer[FinishGame],
    JsonSerializer[Choose],
    JsonSerializer[NextRound],
    JsonSerializer[Chosen],
    JsonSerializer[NextRoundStarted],
    JsonSerializer[GameFinished],
    JsonSerializer[GameStarted],
    JsonSerializer[GetTournament.type],
    JsonSerializer[TournamentDeleted.type],
    JsonSerializer[TournamentFinished.type],
    JsonSerializer[TournamentCreated],
    JsonSerializer[Joined],
    JsonSerializer[TournamentStarted.type],
    JsonSerializer[TournamentUpdated],
    JsonSerializer[UpdateTournament.type ]
  )
}