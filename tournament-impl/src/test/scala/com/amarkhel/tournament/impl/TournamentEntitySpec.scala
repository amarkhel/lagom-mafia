package com.amarkhel.tournament.impl

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.amarkhel.mafia.common.Location
import com.amarkhel.tournament.api._
import com.amarkhel.tournament.impl
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.InvalidCommandException
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver.Reply
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class TournamentEntitySpec extends WordSpecLike with Matchers with BeforeAndAfterAll
  with TypeCheckedTripleEquals {

  val system = ActorSystem("TournamentSpec", JsonSerializerRegistry.actorSystemSetupFor(TournamentSerializerRegistry))
  private val emptyTournament = Tournament("empty", 3, "apa", List.empty, List(GameDescription(1, Location.KRESTY.name, 9, 2, List("a", "b"), 1, None, None)), LocalDateTime.now, None, None, 1)
  override protected def afterAll() : Unit = TestKit.shutdownActorSystem(system)

  "tournament entity" must {
    "handle create validation, empty name" in withEmptyState { driver =>
      checkOutcome(driver.run(CreateTournament(emptyTournament.copy(name = ""))), TournamentEntity.TOURNAMENT_NAME_EMPTY)
    }
    "handle create validation, empty creator" in withEmptyState { driver =>
      checkOutcome(driver.run(CreateTournament(emptyTournament.copy(creator = ""))), TournamentEntity.CREATOR_IS_EMPTY)
    }
    "handle create validation, negative players" in withEmptyState { driver =>
      checkOutcome(driver.run(CreateTournament(emptyTournament.copy(countPlayers = -1, creator = "a"))), TournamentEntity.COUNT_PLAYERS_WRONG)
    }
    "handle create validation, one player" in withEmptyState { driver =>
      checkOutcome(driver.run(CreateTournament(emptyTournament.copy(countPlayers = 1, creator = "a"))), TournamentEntity.COUNT_PLAYERS_WRONG)
    }
    "handle create validation, empty games" in withEmptyState { driver =>
      checkOutcome(driver.run(CreateTournament(emptyTournament.copy(games = List.empty))), TournamentEntity.COUNT_GAMES_EMPTY)
    }
    "handle create validation, too many players" in withEmptyState { driver =>
      checkOutcome(driver.run(CreateTournament(emptyTournament.copy(countPlayers = 21, creator = "a"))), TournamentEntity.COUNT_PLAYERS_WRONG)
    }
    "handle create validation, already started" in withEmptyState { driver =>
      checkOutcome(driver.run(CreateTournament(emptyTournament.copy(creator = "a", start = Some(LocalDateTime.now)))), TournamentEntity.SHOULD_BE_NOT_STARTED_NOT_FINISHED)
    }
    "handle create validation, already finished" in withEmptyState { driver =>
      checkOutcome(driver.run(CreateTournament(emptyTournament.copy(creator = "a", finish = Some(LocalDateTime.now)))), TournamentEntity.SHOULD_BE_NOT_STARTED_NOT_FINISHED)
    }
    "handle create validation, not empty players" in withEmptyState { driver =>
      checkOutcome(driver.run(CreateTournament(emptyTournament.copy(players = List(UserState("apa", List.empty))))), TournamentEntity.PLAYERS_MUST_BE_EMPTY)
    }
    "handle finish tournament in not exist state" in withEmptyState { driver =>
      checkOutcome(driver.run(FinishTournament), TournamentEntity.TOURNAMENT_NOT_FOUND)
    }
    "handle delete tournament in not exist state" in withEmptyState { driver =>
      checkOutcome(driver.run(DeleteTournament), TournamentEntity.TOURNAMENT_NOT_FOUND)
    }
    "handle start tournament in not exist state" in withEmptyState { driver =>
      checkOutcome(driver.run(StartTournament), TournamentEntity.TOURNAMENT_NOT_FOUND)
    }
    "handle update tournament in not exist state" in withEmptyState { driver =>
      checkOutcome(driver.run(UpdateTournament(emptyTournament)), TournamentEntity.TOURNAMENT_NOT_FOUND, checkIssues = false)
    }
    "handle join tournament in not exist state" in withEmptyState { driver =>
      checkOutcome(driver.run(JoinTournament("")), TournamentEntity.TOURNAMENT_NOT_FOUND)
    }
    "handle remove user in not exist state" in withEmptyState { driver =>
      checkOutcome(driver.run(RemoveUser("")), TournamentEntity.TOURNAMENT_NOT_FOUND)
    }
    "handle startGame in not exist state" in withEmptyState { driver =>
      checkOutcome(driver.run(StartGame(1)), TournamentEntity.TOURNAMENT_NOT_FOUND)
    }
    "handle finishGame in not exist state" in withEmptyState { driver =>
      checkOutcome(driver.run(FinishGame(1)), TournamentEntity.TOURNAMENT_NOT_FOUND)
    }
    "handle nextRound in not exist state" in withEmptyState { driver =>
      checkOutcome(driver.run(NextRound("")), TournamentEntity.TOURNAMENT_NOT_FOUND)
    }
    "handle choose in not exist state" in withEmptyState { driver =>
      checkOutcome(driver.run(Choose("", "")), TournamentEntity.TOURNAMENT_NOT_FOUND)
    }
    "handle get tournament in not exist state" in withEmptyState { driver =>
      val outcome = driver.run(GetTournament)
      outcome.issues should be(Nil)
      outcome.events should be(Nil)
      outcome.state should be(None)
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be (Reply(None))
    }
    "handle create tournament in not exist state" in withEmptyState { driver =>
      val outcome = driver.run(CreateTournament(emptyTournament))
      outcome.issues.size should be(0)
      outcome.events.head should be(TournamentCreated(emptyTournament))
      outcome.state should be(Some(emptyTournament))
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be (Reply(emptyTournament))
    }
    "handle finish tournament in not started state" in withCreatedEmpty { driver =>
      checkOutcomeCreatedEmpty(driver.run(FinishTournament), TournamentEntity.TOURNAMENT_NOT_STARTED)
    }
    "handle create tournament in not started state" in withCreatedEmpty { driver =>
      checkOutcomeCreatedEmpty(driver.run(CreateTournament(emptyTournament)), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle start game in not started state" in withCreatedEmpty { driver =>
      checkOutcomeCreatedEmpty(driver.run(StartGame(1)), TournamentEntity.TOURNAMENT_NOT_STARTED)
    }
    "handle finish game in not started state" in withCreatedEmpty { driver =>
      checkOutcomeCreatedEmpty(driver.run(FinishGame(1)), TournamentEntity.TOURNAMENT_NOT_STARTED)
    }
    "handle nextRound in not started state" in withCreatedEmpty { driver =>
      checkOutcomeCreatedEmpty(driver.run(NextRound("")), TournamentEntity.TOURNAMENT_NOT_STARTED)
    }
    "handle choose mafia in not started state" in withCreatedEmpty { driver =>
      checkOutcomeCreatedEmpty(driver.run(impl.Choose("", "")), TournamentEntity.TOURNAMENT_NOT_STARTED)
    }
    "handle get tournament in not started state" in withCreatedEmpty { driver =>
      val outcome = driver.run(GetTournament)
      outcome.issues should be(Nil)
      outcome.events should be(Nil)
      outcome.state should be(Some(emptyTournament))
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(Some(emptyTournament)))
    }
    "handle update tournament in not started state" in withCreated { driver =>
      val descr = GameDescription(1, Location.KRESTY.name, 9, 1, List("a", "b"), 1, Some(LocalDateTime.now))
      val solutions = List(Solution(1, Map("c" -> (1, 15)), 1, false))
      val users = List(UserState("apaapaapa", solutions))
      val tour = Tournament("Tournament", 5, "apaapaapa", users, List(descr), LocalDateTime.now, Some(LocalDateTime.now), None, 1)
      val outcome = driver.run(UpdateTournament(tour))
      outcome.events.head should be(TournamentUpdated(tour))
      outcome.state should be(Some(tour))
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be (Reply(tour))
    }
    "handle start tournament in not started state" in withCreated { driver =>
      val outcome = driver.run(StartTournament)
      outcome.events.head should be(TournamentStarted)
      outcome.state.get.start.isDefined should be (true)
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be (Reply(true))
    }
    "handle start tournament in not started state and not joined" in withCreatedEmpty { driver =>
      val outcome = driver.run(StartTournament)
      outcome.events.size should be(0)
      outcome.state.get.start.isDefined should be (false)
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.NOT_ALL_PLAYERS_JOINED)))
    }
    "handle remove user in not started state and not joined" in withCreatedEmpty { driver =>
      val outcome = driver.run(RemoveUser("puf"))
      outcome.events.size should be(0)
      outcome.state.get.players.size should be (0)
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.NOT_JOINED)))
    }
    "handle remove user in not started state and joined" in withCreated { driver =>
      val outcome = driver.run(RemoveUser("p"))
      outcome.events.size should be(0)
      outcome.state.get.players.size should be (3)
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.NOT_JOINED)))
      val outcome2 = driver.run(RemoveUser("puf"))
      outcome2.events.head should be(UserRemoved("puf"))
      outcome2.state.get.players.size should be (2)
      outcome2.sideEffects.size should be(1)
      outcome2.sideEffects.head should be (Reply(true))
      val outcome3 = driver.run(RemoveUser("puf"))
      outcome3.events.size should be(0)
      outcome3.state.get.players.size should be (2)
      outcome3.sideEffects.size should be(1)
      outcome3.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.NOT_JOINED)))
    }
    "handle join tournament in not started state" in withCreatedEmpty { driver =>
      val outcome5 = driver.run(JoinTournament(""))
      outcome5.events should be(Nil)
      outcome5.state.get.players.size should be (0)
      outcome5.sideEffects.size should be(1)
      outcome5.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.BLANK_NAME)))
      val outcome = driver.run(JoinTournament("a"))
      outcome.events.head should be(Joined("a"))
      outcome.state.get.players.size should be (1)
      outcome.state.get.players.head.name should be ("a")
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be (Reply(true))
      val outcome2 = driver.run(JoinTournament("a"))
      outcome2.events should be(Nil)
      outcome2.state.get.players.size should be (1)
      outcome2.sideEffects.size should be(1)
      outcome2.state.get.players.head.name should be ("a")
      outcome2.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.ALREADY_JOINED)))
      val outcome4 = driver.run(JoinTournament("c"))
      outcome4.events.head should be(Joined("c"))
      outcome4.state.get.players.size should be (2)
      outcome4.state.get.players.head.name should be ("c")
      outcome4.state.get.players.last.name should be ("a")
      outcome4.sideEffects.size should be(1)
      outcome4.sideEffects.head should be (Reply(true))
      val outcome3 = driver.run(JoinTournament("b"))
      outcome3.events should be(Vector(Joined("b")))
      outcome3.state.get.players.size should be (3)
      outcome3.sideEffects.size should be(1)
      outcome3.state.get.players.head.name should be ("b")
      outcome3.state.get.players.last.name should be ("a")
      outcome3.sideEffects.head should be (Reply(true))
      val outcome6 = driver.run(JoinTournament("d"))
      outcome6.events should be(Nil)
      outcome6.state.get.players.size should be (3)
      outcome6.sideEffects.size should be(1)
      outcome6.state.get.players.head.name should be ("b")
      outcome6.state.get.players.last.name should be ("a")
      outcome6.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.ALL_PEOPLE_JOINED)))
    }
    "handle finish tournament in finished state" in withFinished { driver =>
      checkOutcomeFinished(driver.run(FinishTournament), TournamentEntity.TOURNAMENT_FINISHED)
    }
    "handle update tournament in finished state" in withFinished { driver =>
      checkOutcomeFinished(driver.run(UpdateTournament(emptyTournament)), TournamentEntity.TOURNAMENT_FINISHED, checkIssues = false)
    }
    "handle create tournament in finished state" in withFinished { driver =>
      checkOutcomeFinished(driver.run(CreateTournament(emptyTournament)), TournamentEntity.TOURNAMENT_FINISHED)
    }
    "handle delete tournament in finished state" in withFinished { driver =>
      checkOutcomeFinished(driver.run(DeleteTournament), TournamentEntity.TOURNAMENT_FINISHED)
    }
    "handle join tournament in finished state" in withFinished { driver =>
      checkOutcomeFinished(driver.run(JoinTournament("a")), TournamentEntity.TOURNAMENT_FINISHED)
    }
    "handle remove user in finished state" in withFinished { driver =>
      checkOutcomeFinished(driver.run(RemoveUser("a")), TournamentEntity.TOURNAMENT_FINISHED)
    }
    "handle finish game in finished state" in withFinished { driver =>
      checkOutcomeFinished(driver.run(FinishGame(1)), TournamentEntity.TOURNAMENT_FINISHED)
    }
    "handle start game in finished state" in withFinished { driver =>
      checkOutcomeFinished(driver.run(StartGame(1)), TournamentEntity.TOURNAMENT_FINISHED)
    }
    "handle chose mafia in finished state" in withFinished { driver =>
      checkOutcomeFinished(driver.run(Choose("a", "b")), TournamentEntity.TOURNAMENT_FINISHED)
    }
    "handle next round in finished state" in withFinished { driver =>
      checkOutcomeFinished(driver.run(NextRound("a")), TournamentEntity.TOURNAMENT_FINISHED)
    }
    "handle get tournament in finished state" in withFinished { driver =>
      val outcome = driver.run(GetTournament)
      outcome.issues should be(Nil)
      outcome.events should be(Nil)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(true)
      outcome.state.get.finished should be(true)
      outcome.state.get.inProgress should be(false)
      outcome.state.get.hasGameInProgress should be(false)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(outcome.state))
    }
    "handle delete tournament in started state" in withStarted { driver =>
      checkOutcomeStarted(driver.run(DeleteTournament), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle update tournament in started state" in withStarted { driver =>
      checkOutcomeStarted(driver.run(UpdateTournament(emptyTournament)), TournamentEntity.TOURNAMENT_ALREADY_STARTED, checkIssues = false)
    }
    "handle create tournament in started state" in withStarted { driver =>
      checkOutcomeStarted(driver.run(CreateTournament(emptyTournament)), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle join tournament in started state" in withStarted { driver =>
      checkOutcomeStarted(driver.run(JoinTournament("a")), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle remove user in started state" in withStarted { driver =>
      checkOutcomeStarted(driver.run(RemoveUser("a")), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle finish game in started state" in withStarted { driver =>
      checkOutcomeStarted(driver.run(FinishGame(1)), TournamentEntity.NO_CURRENT_GAME)
    }
    "handle choose mafia in started state" in withStarted { driver =>
      checkOutcomeStarted(driver.run(Choose("", "")), TournamentEntity.NO_CURRENT_GAME)
    }
    "handle next round in started state" in withStarted { driver =>
      checkOutcomeStarted(driver.run(NextRound("")), TournamentEntity.NO_CURRENT_GAME)
    }
    "handle get tournament in started state" in withStarted { driver =>
      val outcome = driver.run(GetTournament)
      outcome.issues should be(Nil)
      outcome.events.size should be(0)
      outcome.events should be(Nil)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.hasGameInProgress should be(false)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(Some(emptyTournament.copy(start = outcome.state.get.start, players = List(UserState("unstop",List()), UserState("never",List()), UserState("puf",List()))))))
    }
    "handle finish tournament in started state and played" in withStartedAndPlayed { driver =>
      val outcome = driver.run(FinishTournament)
      outcome.issues should be(Nil)
      outcome.events.size should be(1)
      outcome.events.head should be(TournamentFinished)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(true)
      outcome.state.get.finished should be(true)
      outcome.state.get.inProgress should be(false)
      outcome.state.get.hasGameInProgress should be(false)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(true))
    }
    "handle finish tournament in started state" in withStarted { driver =>
      val outcome = driver.run(FinishTournament)
      outcome.issues should be(Nil)
      outcome.events.size should be(0)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.hasGameInProgress should be(false)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.NOT_ALL_GAMES_FINISHED)))
    }
    "handle start game in started state" in withStarted { driver =>
      val outcome = driver.run(StartGame(1))
      outcome.issues should be(Nil)
      outcome.events.size should be(1)
      outcome.events.head should be(GameStarted(1))
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.hasGameInProgress should be(true)
      outcome.state.get.gameInProgress.get.id should be(1)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(true))
    }
    "handle errors in start game in started state" in withStarted { driver =>
      val outcome = driver.run(StartGame(1000))
      outcome.issues should be(Nil)
      outcome.events.size should be(0)
      outcome.events should be(Nil)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.hasGameInProgress should be(false)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.GAME_NOT_FOUND)))
    }
    "handle errors in started state 3" in withState { driver =>
      driver.run(CreateTournament(emptyTournament.copy(games = GameDescription(0, Location.OZHA.name, 12, 21, List("a", "b"), 1, None, None) :: emptyTournament.games)))
      driver.run(JoinTournament("puf"))
      driver.run(JoinTournament("never"))
      driver.run(JoinTournament("unstop"))
      driver.run(StartTournament)
      driver.run(StartGame(1))
      driver.run(FinishGame(1))
      val outcome = driver.run(StartGame(1))
      outcome.issues should be(Nil)
      outcome.events.size should be(0)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.hasGameInProgress should be(false)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.GAME_WAS_STARTED)))
    }
    "handle errors in started state 2" in withStarted { driver =>
      driver.run(StartGame(1))
      val outcome = driver.run(StartGame(1))
      outcome.issues should be(Nil)
      outcome.events.size should be(0)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.hasGameInProgress should be(true)
      outcome.state.get.gameInProgress.get.id should be(1)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.GAME_IN_PROGRESS)))
    }
    "handle delete tournament in started game state" in withStartedGame { driver =>
      checkOutcomeStartedGame(driver.run(DeleteTournament), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle finish tournament in started game state" in withStartedGame { driver =>
      checkOutcomeStartedGame(driver.run(FinishTournament), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle start tournament in started game state" in withStartedGame { driver =>
      checkOutcomeStartedGame(driver.run(StartTournament), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle update tournament in started game state" in withStartedGame { driver =>
      checkOutcomeStartedGame(driver.run(UpdateTournament(emptyTournament)), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle create tournament in started game state" in withStartedGame { driver =>
      checkOutcomeStartedGame(driver.run(CreateTournament(emptyTournament)), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle join tournament in started game state" in withStartedGame { driver =>
      checkOutcomeStartedGame(driver.run(JoinTournament("")), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle remove user in started game state" in withStartedGame { driver =>
      checkOutcomeStartedGame(driver.run(RemoveUser("")), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle start game in started game state" in withStartedGame { driver =>
      checkOutcomeStartedGame(driver.run(StartGame(1)), TournamentEntity.GAME_IN_PROGRESS)
    }
    "handle get tournament in started game state" in withStartedGame { driver =>
      val outcome = driver.run(GetTournament)
      outcome.issues should be(Nil)
      outcome.events.size should be(0)
      outcome.events should be(Nil)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.games.head.started.isDefined should be(true)
      outcome.state.get.hasGameInProgress should be(true)
      outcome.sideEffects.size should be (1)
    }
    "handle finish game in started game state" in withStartedGame { driver =>
      val outcome = driver.run(FinishGame(1))
      outcome.issues should be(Nil)
      outcome.events.size should be(1)
      outcome.events.head should be(GameFinished(1))
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.games.head.started.isDefined should be(true)
      outcome.state.get.games.head.finished.isDefined should be(true)
      outcome.state.get.hasGameInProgress should be(false)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(true))
    }
    "handle finish wrong game in started game state" in withStartedGame { driver =>
      val outcome = driver.run(FinishGame(2))
      outcome.issues should be(Nil)
      outcome.events.size should be(0)
      outcome.events should be(Nil)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.games.head.started.isDefined should be(true)
      outcome.state.get.hasGameInProgress should be(true)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.ANOTHER_GAME_IN_PROGRESS)))
    }
    "handle chosen in started game state wrong" in withStartedGame { driver =>
      val outcome = driver.run(Choose("a", "b"))
      outcome.issues should be(Nil)
      outcome.events.size should be(0)
      outcome.events should be(Nil)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.games.head.started.isDefined should be(true)
      outcome.state.get.hasGameInProgress should be(true)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.PLAYER_NOT_JOINED_TO_TOURNAMENT)))
    }
    "handle chosen in started game state" in withStartedGame { driver =>
      val outcome = driver.run(Choose("never", "c"))
      outcome.issues should be(Nil)
      outcome.events.size should be(1)
      outcome.events.head should be(Chosen("never", "c", 1))
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.games.head.started.isDefined should be(true)
      outcome.state.get.hasGameInProgress should be(true)
      outcome.state.get.players.filter(_.name == "never").head.solutions.head.mafia.size should be (1)
      outcome.state.get.players.filter(_.name == "never").head.solutions.head.mafia.get("c").get should be (0,0)
      outcome.sideEffects.size should be (1)
      val out = driver.run(Choose("never", "c"))
      out.issues should be(Nil)
      out.events.size should be(0)
      out.events should be(Nil)
      out.state.get.start.isDefined should be(true)
      out.state.get.finish.isDefined should be(false)
      out.state.get.finished should be(false)
      out.state.get.inProgress should be(true)
      out.state.get.games.head.started.isDefined should be(true)
      out.state.get.hasGameInProgress should be(true)
      out.state.get.players.filter(_.name == "never").head.solutions.head.mafia.size should be (1)
      out.state.get.players.filter(_.name == "never").head.solutions.head.mafia.get("c").get should be (0,0)
      out.sideEffects.size should be (1)
      out.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.ALREADY_VOTED_FOR_THIS_PLAYER)))
      driver.run(Choose("never", "b"))
      val out2 = driver.run(Choose("never", "d"))
      out2.issues should be(Nil)
      out2.events.size should be(0)
      out2.events should be(Nil)
      out2.state.get.start.isDefined should be(true)
      out2.state.get.finish.isDefined should be(false)
      out2.state.get.finished should be(false)
      out2.state.get.inProgress should be(true)
      out2.state.get.games.head.started.isDefined should be(true)
      out2.state.get.hasGameInProgress should be(true)
      out2.state.get.players.filter(_.name == "never").head.solutions.head.mafia.size should be (2)
      out2.state.get.players.filter(_.name == "never").head.solutions.head.mafia.get("c").get should be (0,0)
      out2.sideEffects.size should be (1)
      out2.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.ALL_MAFIA_CHOSEN)))
    }
    "handle next round in started game state" in withStartedGame { driver =>
      val outcome = driver.run(NextRound(""))
      outcome.issues should be(Nil)
      outcome.events.size should be(0)
      outcome.events should be(Nil)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.games.head.started.isDefined should be(true)
      outcome.state.get.hasGameInProgress should be(true)
      outcome.state.get.players.head.solutions.head.currentRound should be (0)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.PLAYER_NOT_JOINED_TO_TOURNAMENT)))
    }
    "handle next round in started game state 2" in withStartedGame { driver =>
      val outcome = driver.run(NextRound("never"))
      outcome.issues should be(Nil)
      outcome.events.size should be(1)
      outcome.events.head should be(NextRoundStarted("never", 1))
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.games.head.started.isDefined should be(true)
      outcome.state.get.hasGameInProgress should be(true)
      outcome.state.get.players.filter(_.name == "never").head.solutions.head.currentRound should be (1)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(true))
      val outcome2 = driver.run(NextRound("never"))
      outcome2.events.head should be(NextRoundStarted("never", 1))
      outcome2.state.get.players.filter(_.name == "never").head.solutions.head.currentRound should be (2)
      val outcome3 = driver.run(NextRound("never"))
      outcome3.issues should be(Nil)
      outcome3.events.size should be(0)
      outcome3.events should be(Nil)
      outcome3.state.get.start.isDefined should be(true)
      outcome3.state.get.finish.isDefined should be(false)
      outcome3.state.get.finished should be(false)
      outcome3.state.get.inProgress should be(true)
      outcome3.state.get.games.head.started.isDefined should be(true)
      outcome3.state.get.hasGameInProgress should be(true)
      outcome3.state.get.players.filter(_.name == "never").head.solutions.head.currentRound should be (2)
      outcome3.sideEffects.size should be (1)
      outcome3.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.LAST_ROUND_ALREADY_CHOSEN)))
    }
    def checkOutcome[A, B](outcome: PersistentEntityTestDriver.Outcome[A, B], message: String, checkIssues:Boolean=true): Any = {
      if(checkIssues) outcome.issues should be(Nil)
      outcome.events should be(Nil)
      outcome.state should be(None)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(message)))
    }
    def checkOutcomeStartedGame[A](outcome: PersistentEntityTestDriver.Outcome[A, Option[Tournament]], message: String, checkIssues:Boolean=true): Any = {
      if(checkIssues) outcome.issues should be(Nil)
      outcome.events should be(Nil)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.hasGameInProgress should be(true)
      outcome.state.get.games.head.started.isDefined should be(true)
      outcome.state.get.games.head.finished.isDefined should be(false)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(message)))
    }
    def checkOutcomeStarted[A](outcome: PersistentEntityTestDriver.Outcome[A, Option[Tournament]], message: String, checkIssues:Boolean=true): Any = {
      if(checkIssues) outcome.issues should be(Nil)
      outcome.events should be(Nil)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.hasGameInProgress should be(false)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(message)))
    }
    def checkOutcomeCreated[A, B](outcome: PersistentEntityTestDriver.Outcome[A, B], message: String, checkIssues:Boolean=true): Any = {
      if(checkIssues) outcome.issues should be(Nil)
      outcome.events should be(Nil)
      outcome.state should be(Some(emptyTournament.copy(players = List(UserState("unstop",List()), UserState("never",List()), UserState("puf",List())))))
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(message)))
    }
    def checkOutcomeCreatedEmpty[A, B](outcome: PersistentEntityTestDriver.Outcome[A, B], message: String, checkIssues:Boolean=true): Any = {
      if(checkIssues) outcome.issues should be(Nil)
      outcome.events should be(Nil)
      outcome.state should be(Some(emptyTournament))
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(message)))
    }
    def checkOutcomeFinished[A](outcome: PersistentEntityTestDriver.Outcome[A, Option[Tournament]], message: String, checkIssues:Boolean=true): Any = {
      if(checkIssues) outcome.issues should be(Nil)
      outcome.events should be(Nil)
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(true)
      outcome.state.get.finished should be(true)
      outcome.state.get.inProgress should be(false)
      outcome.state.get.hasGameInProgress should be(false)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(InvalidCommandException(message)))
    }
    def withEmptyState(block: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit): Unit = {
      withState(block)
    }
    def withCreatedEmpty(block: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit): Unit = {
      withState(block, _.run(CreateTournament(emptyTournament)))
    }
    def withCreated(block: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit): Unit = {
      withState(block, d => {
        d.run(CreateTournament(emptyTournament))
        d.run(JoinTournament("puf"))
        d.run(JoinTournament("never"))
        d.run(JoinTournament("unstop"))
      })
    }
    def withFinished(block: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit): Unit = {
      withState(block, d => {
        d.run(CreateTournament(emptyTournament))
        d.run(JoinTournament("puf"))
        d.run(JoinTournament("never"))
        d.run(JoinTournament("unstop"))
        d.run(StartTournament)
        d.run(StartGame(1))
        d.run(Choose("puf", "b"))
        d.run(Choose("puf", "c"))
        d.run(Choose("never", "b"))
        d.run(Choose("never", "a"))
        d.run(Choose("unstop", "b"))
        d.run(Choose("unstop", "f"))
        d.run(FinishGame(1))
        d.run(FinishTournament)
      })
    }
    def withStarted(block: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit): Unit = {
      withState(block, d => {
        d.run(CreateTournament(emptyTournament))
        d.run(JoinTournament("puf"))
        d.run(JoinTournament("never"))
        d.run(JoinTournament("unstop"))
        d.run(StartTournament)
      })
    }
    def withStartedAndPlayed(block: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit): Unit = {
      withState(block, d => {
        d.run(CreateTournament(emptyTournament))
        d.run(JoinTournament("puf"))
        d.run(JoinTournament("never"))
        d.run(JoinTournament("unstop"))
        d.run(StartTournament)
        d.run(StartGame(1))
        d.run(Choose("puf", "b"))
        d.run(Choose("puf", "c"))
        d.run(Choose("never", "b"))
        d.run(Choose("never", "a"))
        d.run(Choose("unstop", "b"))
        d.run(Choose("unstop", "f"))
        d.run(FinishGame(1))
      })
    }
    def withStartedGame(block: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit): Unit = {
      withState(block, d => {
        d.run(CreateTournament(emptyTournament))
        d.run(JoinTournament("puf"))
        d.run(JoinTournament("never"))
        d.run(JoinTournament("unstop"))
        d.run(StartTournament)
        d.run(StartGame(1))
      })
    }
    def withState(block: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit, init: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit = _ => ()): Unit = {
      val driver = new PersistentEntityTestDriver(system, new TournamentEntity, "1")
      init(driver)
      block(driver)
      if (driver.getAllIssues.nonEmpty) {
        driver.getAllIssues.foreach(println)
        fail("There were issues " + driver.getAllIssues.head)
      }
    }
  }
}
