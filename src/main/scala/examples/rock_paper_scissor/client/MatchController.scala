package examples.rock_paper_scissor.client

import client.room.ClientRoom
import javafx.scene.control.{Label, SplitPane}
import javafx.scene.layout.GridPane
import javafx.{event => jfxEvent, fxml => jfxf}
import scalafx.application.Platform
import javafx.{fxml => jfxf}
import javafx.{scene => jfxs}


class MatchController {
  @jfxf.FXML var labelWaitingPlayer: Label = _
  @jfxf.FXML var splitPaneButtons: SplitPane = _
  @jfxf.FXML var gridPaneAdvancedMoves: GridPane = _

  private var room: ClientRoom = _

  def init(room: ClientRoom, gameType: String): Unit = {
    this.splitPaneButtons.setVisible(false)
    gameType match {
      case "classic" => this.gridPaneAdvancedMoves.setDisable(true)
      case "advanced"=> this.gridPaneAdvancedMoves.setDisable(false)
    }
    this.room = room
    room.onMessageReceived {
      //ready to play
      case "start" =>
        Platform.runLater {
          this.labelWaitingPlayer.setText("Play!")
          this.splitPaneButtons.setVisible(true)
        }

      //game result: win, lose or draw
      case msg =>
        Platform.runLater {
          val loader = new jfxf.FXMLLoader(getClass.getResource("./resources/game_end.fxml"))
          val root: jfxs.Parent = loader.load()
          loader.getController[GameEndController]().init(msg.toString)
          this.splitPaneButtons.getScene.setRoot(root)
        }

    }

  }

  @jfxf.FXML
  def handleRockButtonPress(event: jfxEvent.ActionEvent): Unit = {
    makeMove("rock")
  }

  @jfxf.FXML
  def handlePaperButtonPress(event: jfxEvent.ActionEvent): Unit = {
    makeMove("paper")
  }

  @jfxf.FXML
  def handleScissorButtonPress(event: jfxEvent.ActionEvent): Unit = {
    makeMove("scissor")
  }

  @jfxf.FXML
  def handleLizardButtonPress(event: jfxEvent.ActionEvent): Unit = {
    makeMove("lizard")
  }

  @jfxf.FXML
  def handleSpockButtonPress(event: jfxEvent.ActionEvent): Unit = {
    makeMove("spock")
  }

  private def makeMove(move: String): Unit = {
    room.send(move)
    Platform.runLater {
      this.labelWaitingPlayer.setText("You played -> " + move)
      this.splitPaneButtons.setDisable(true)
    }
  }


}



