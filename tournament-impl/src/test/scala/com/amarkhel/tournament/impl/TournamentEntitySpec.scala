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
  private val emptyTournament = Tournament("empty", 3, "", List(UserState("apa", List.empty)), List(GameDescription(1, Location.KRESTY.name, 9, 2, List("a", "b"), 1, None, None)), LocalDateTime.now, None, None, 1)
  override protected def afterAll() : Unit = TestKit.shutdownActorSystem(system)

  "tournament entity" must {
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
    "handle create tournament in not exist state 2" in withEmptyState { driver =>
      val results = List(GameDescription(1, Location.KRESTY.name, 9, 1, List("a", "b"), 1, Some(LocalDateTime.now)))
      val users = List(UserState("apaapaapa", List.empty))
      val tour = Tournament("Tournament", 5, "apaapaapa", users, results, LocalDateTime.now, Some(LocalDateTime.now), None, 1)
      val outcome = driver.run(CreateTournament(tour))
      outcome.issues.size should be(0)
      outcome.events.head should be(TournamentCreated(tour))
      outcome.state should be(Some(tour))
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be (Reply(tour))
    }
    "handle finish tournament in not started state" in withCreated { driver =>
      checkOutcomeCreated(driver.run(FinishTournament), TournamentEntity.TOURNAMENT_NOT_STARTED)
    }
    "handle create tournament in not started state" in withCreated { driver =>
      checkOutcomeCreated(driver.run(CreateTournament(emptyTournament)), TournamentEntity.TOURNAMENT_ALREADY_STARTED)
    }
    "handle start game in not started state" in withCreated { driver =>
      checkOutcomeCreated(driver.run(StartGame(1)), TournamentEntity.TOURNAMENT_NOT_STARTED)
    }
    "handle finish game in not started state" in withCreated { driver =>
      checkOutcomeCreated(driver.run(FinishGame(1)), TournamentEntity.TOURNAMENT_NOT_STARTED)
    }
    "handle nextRound in not started state" in withCreated { driver =>
      checkOutcomeCreated(driver.run(NextRound("")), TournamentEntity.TOURNAMENT_NOT_STARTED)
    }
    "handle choose mafia in not started state" in withCreated { driver =>
      checkOutcomeCreated(driver.run(impl.Choose("", "")), TournamentEntity.TOURNAMENT_NOT_STARTED)
    }
    "handle get tournament in not started state" in withCreated { driver =>
      val outcome = driver.run(GetTournament)
      outcome.issues should be(Nil)
      outcome.events should be(Nil)
      outcome.state should be(Some(emptyTournament))
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(Some(emptyTournament)))
    }
    "handle update tournament in not started state" in withCreated { driver =>
      val descr = GameDescription(1, Location.KRESTY.name, 9, 1, List("a", "b"), 1, Some(LocalDateTime.now))
      val solutions = List(Solution(1, Map("c" -> 1), 1, false))
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
    "handle join tournament in not started state" in withCreated { driver =>
      val outcome5 = driver.run(JoinTournament(""))
      outcome5.events should be(Nil)
      outcome5.state.get.players.size should be (1)
      outcome5.sideEffects.size should be(1)
      outcome5.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.BLANK_NAME)))
      val outcome = driver.run(JoinTournament("a"))
      outcome.events.head should be(Joined("a"))
      outcome.state.get.players.size should be (2)
      outcome.state.get.players.head.name should be ("a")
      outcome.sideEffects.size should be(1)
      outcome.sideEffects.head should be (Reply(true))
      val outcome2 = driver.run(JoinTournament("a"))
      outcome2.events should be(Nil)
      outcome2.state.get.players.size should be (2)
      outcome2.sideEffects.size should be(1)
      outcome2.state.get.players.head.name should be ("a")
      outcome2.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.ALREADY_JOINED)))
      val outcome4 = driver.run(JoinTournament("c"))
      outcome4.events.head should be(Joined("c"))
      outcome4.state.get.players.size should be (3)
      outcome4.state.get.players.head.name should be ("c")
      outcome4.state.get.players.last.name should be ("apa")
      outcome4.sideEffects.size should be(1)
      outcome4.sideEffects.head should be (Reply(true))
      val outcome3 = driver.run(JoinTournament("b"))
      outcome3.events should be(Nil)
      outcome3.state.get.players.size should be (3)
      outcome3.sideEffects.size should be(1)
      outcome3.state.get.players.head.name should be ("c")
      outcome3.state.get.players.last.name should be ("apa")
      outcome3.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.ALL_PEOPLE_JOINED)))
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
      outcome.sideEffects.head should be (Reply(Some(emptyTournament.copy(start = outcome.state.get.start))))
    }
    "handle finish tournament in started state" in withStarted { driver =>
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
      val outcome = driver.run(Choose("apa", "c"))
      outcome.issues should be(Nil)
      outcome.events.size should be(1)
      outcome.events.head should be(Chosen("apa", "c", 1))
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.games.head.started.isDefined should be(true)
      outcome.state.get.hasGameInProgress should be(true)
      outcome.state.get.players.head.solutions.head.mafia.size should be (1)
      outcome.state.get.players.head.solutions.head.mafia.get("c").get should be (0)
      outcome.sideEffects.size should be (1)
      val out = driver.run(Choose("apa", "c"))
      out.issues should be(Nil)
      out.events.size should be(0)
      out.events should be(Nil)
      out.state.get.start.isDefined should be(true)
      out.state.get.finish.isDefined should be(false)
      out.state.get.finished should be(false)
      out.state.get.inProgress should be(true)
      out.state.get.games.head.started.isDefined should be(true)
      out.state.get.hasGameInProgress should be(true)
      out.state.get.players.head.solutions.head.mafia.size should be (1)
      out.state.get.players.head.solutions.head.mafia.get("c").get should be (0)
      out.sideEffects.size should be (1)
      out.sideEffects.head should be (Reply(InvalidCommandException(TournamentEntity.ALREADY_VOTED_FOR_THIS_PLAYER)))
      driver.run(Choose("apa", "b"))
      val out2 = driver.run(Choose("apa", "d"))
      out2.issues should be(Nil)
      out2.events.size should be(0)
      out2.events should be(Nil)
      out2.state.get.start.isDefined should be(true)
      out2.state.get.finish.isDefined should be(false)
      out2.state.get.finished should be(false)
      out2.state.get.inProgress should be(true)
      out2.state.get.games.head.started.isDefined should be(true)
      out2.state.get.hasGameInProgress should be(true)
      out2.state.get.players.head.solutions.head.mafia.size should be (2)
      out2.state.get.players.head.solutions.head.mafia.get("c").get should be (0)
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
      val outcome = driver.run(NextRound("apa"))
      outcome.issues should be(Nil)
      outcome.events.size should be(1)
      outcome.events.head should be(NextRoundStarted("apa", 1))
      outcome.state.get.start.isDefined should be(true)
      outcome.state.get.finish.isDefined should be(false)
      outcome.state.get.finished should be(false)
      outcome.state.get.inProgress should be(true)
      outcome.state.get.games.head.started.isDefined should be(true)
      outcome.state.get.hasGameInProgress should be(true)
      outcome.state.get.players.head.solutions.head.currentRound should be (1)
      outcome.sideEffects.size should be (1)
      outcome.sideEffects.head should be (Reply(true))
      val outcome2 = driver.run(NextRound("apa"))
      outcome2.events.head should be(NextRoundStarted("apa", 1))
      outcome2.state.get.players.head.solutions.head.currentRound should be (2)
      val outcome3 = driver.run(NextRound("apa"))
      outcome3.issues should be(Nil)
      outcome3.events.size should be(0)
      outcome3.events should be(Nil)
      outcome3.state.get.start.isDefined should be(true)
      outcome3.state.get.finish.isDefined should be(false)
      outcome3.state.get.finished should be(false)
      outcome3.state.get.inProgress should be(true)
      outcome3.state.get.games.head.started.isDefined should be(true)
      outcome3.state.get.hasGameInProgress should be(true)
      outcome3.state.get.players.head.solutions.head.currentRound should be (2)
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
    def withCreated(block: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit): Unit = {
      withState(block, _.run(CreateTournament(emptyTournament)))
    }
    def withFinished(block: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit): Unit = {
      withState(block, d => {
        d.run(CreateTournament(emptyTournament))
        d.run(StartTournament)
        d.run(FinishTournament)
      })
    }
    def withStarted(block: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit): Unit = {
      withState(block, d => {
        d.run(CreateTournament(emptyTournament))
        d.run(StartTournament)
      })
    }
    def withStartedGame(block: PersistentEntityTestDriver[TournamentCommand, TournamentEvent, Option[Tournament]] => Unit): Unit = {
      withState(block, d => {
        d.run(CreateTournament(emptyTournament))
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
