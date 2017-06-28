import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.control.Tooltip
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.Stage

/**
 * @author Dominik Elflein
 */
class NewsScraperMain extends Application{

    static TextArea log = new TextArea()
    static Log logger = new Log()

    static DatabaseController db = new DatabaseController()
    static RegisterScraper register = new RegisterScraper()

    static List<ScraperJob> runningScrapers = []

    static void main(String[] args){
        launch(this, args)
    }

    /**
     * Creates the Graphical User Interface
     *
     * @param primaryStage
     * @throws Exception
     */
    @Override
    void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("News Scraper Application")

        //LayoutManager
        BorderPane borderPane = new BorderPane()

        log.setEditable(false)
        log.setPrefHeight(125)
        borderPane.setBottom(log)

        VBox leftPanel = new VBox(5)
        leftPanel.setAlignment(Pos.CENTER_LEFT)
        List<Button> buttons = []
        register.scrapers.each { scraper ->
            Button button = new Button(scraper.scraperName)
            button.setPrefWidth(120)

            button.setTooltip(new Tooltip(scraper.scraperName))
            button.setOnAction { e ->
                //TODO Confirmation Dialog
                runningScrapers << scraper
                Thread.start {
                    scraper.runScraper()
                }
            }
            buttons << button
        }
        leftPanel.getChildren().addAll(buttons)

        BorderPane.setMargin(leftPanel, new Insets(4,4,4,4))
        borderPane.setLeft(leftPanel)

        VBox rightPanel = new VBox()
        rightPanel.setAlignment(Pos.CENTER_RIGHT)
        //Button pause = new Button("Pause")
        /*Button autoPlay = new Button("AutoMode")
        autoPlay.setOnAction { e ->
            register.scrapers.each { scraper ->
                if(scraper.isStopped()){
                    Thread.start {
                        scraper.runScraper()
                    }
                }
            }
        }*/
        //rightPanel.getChildren().add(autoPlay)
        BorderPane.setMargin(rightPanel, new Insets(4,4,4,4))
        borderPane.setRight(rightPanel)


        Scene scene = new Scene(borderPane)

        primaryStage.setScene(scene)

        primaryStage.setWidth(800)
        primaryStage.setHeight(600)
        primaryStage.setResizable(false)
        primaryStage.show()
    }

    /**
     * Gets called after Exit Code is fired, or all Windows are closed.
     */
    @Override
    void stop(){
        if(runningScrapers.size() > 0){
            runningScrapers.each {
                it.stopped = true
            }
        }
    }
}
