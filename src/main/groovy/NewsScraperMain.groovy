import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage

import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException

/**
 * @author Dominik Elflein
 */
class NewsScraperMain extends Application{

    static TextArea log = new TextArea()
    static Log logger = new Log()
    static TextArea resultArea

    static DatabaseController db = new DatabaseController()
    static RegisterScraper register = new RegisterScraper()

    static List<ScraperJob> runningScrapers = []

    synchronized boolean stop = true

    static List<ScraperResult> currentSearch = null

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

        /* ---------------------------
        CenterPanel
         */
        VBox centerPanel = new VBox(5)
        centerPanel.setAlignment(Pos.CENTER)
        HBox searchLayer = new HBox(5)
        TextField searchBox = new TextField("Search ...")
        Button searchBtn = new Button("Search")
        searchBtn.setOnAction { e->
            if(searchBox.getText() != "Search ..."){
               searchDatabase(searchBox.getText())
            }
        }
        searchLayer.getChildren().addAll(searchBox, searchBtn)

        resultArea = new TextArea("Currently no search result.")
        resultArea.setEditable(true)

        HBox exportLayer = new HBox(5)
        Button exportCSV = new Button("Export CSV")
        exportCSV.setOnAction { e->
            exportCSVFile(searchBox.getText())
        }
        exportLayer.getChildren().addAll(exportCSV)

        centerPanel.getChildren().addAll(searchLayer, resultArea, exportLayer)
        borderPane.setCenter(centerPanel)
        //----------------------------

        VBox rightPanel = new VBox(5)
        rightPanel.setAlignment(Pos.CENTER_RIGHT)
        Button pause = new Button("Pause")
        pause.setPrefWidth(120)
        pause.setOnAction { e ->
            stop = true
        }
        Button autoPlay = new Button("AutoMode")
        autoPlay.setOnAction { e ->
            Thread.start {

                List<ScraperJob> availableScraper = [
                        new BildScraper("Bild"),
                        new FocusScraper("Focus"),
                        new SpiegelScraper("Spiegel"),
                        new SueddeutscheScraper("Sueddeutsche"),
                        new WeltScraper("Welt")
                ]

                logger.info("Preparing Autorun")
                if(db == null){
                    logger.info("No Database Connection...")
                }
                stop = false
                while(!stop){
                    availableScraper.each {
                        if(!stop){
                            logger.info("Launching $it.scraperName ...")
                            it.runScraper()
                        }else{
                            logger.warn("Autorun stopped manually.")
                        }
                    }
                    logger.info("Pausing program for 8 hours ...")
                    long timeLimit = 8*3600*1000 // 8 Stunden
                    long pastTime = 0L
                    long warnTime = 30*60*1000 // 30 Minuten

                    while(pastTime < timeLimit){
                        if(!stop){
                            sleep(warnTime)
                            pastTime+= warnTime
                            long leftTime = timeLimit - pastTime
                            double timeHourLeft = (((leftTime/1000)/60)/60)
                            logger.info("$timeHourLeft hours, until next autoRun.")
                        }else{
                            logger.warn("Autorun stopped manually.")
                        }
                    }
                    logger.info("Starting next autoRun.")
                }
                logger.warn("Autorun stopped manually.")
            }
        }
        autoPlay.setPrefWidth(120)
        rightPanel.getChildren().add(autoPlay)
        rightPanel.getChildren().add(pause)
        BorderPane.setMargin(rightPanel, new Insets(4,4,4,4))
        borderPane.setRight(rightPanel)


        Scene scene = new Scene(borderPane)

        primaryStage.setScene(scene)

        primaryStage.setWidth(800)
        primaryStage.setHeight(600)
        primaryStage.setResizable(false)
        primaryStage.show()
    }

    private static final String CSV_SEPARATOR = ";"
    static exportCSVFile(String search) {
        try{
            File file = new File("./export")
            if(!file.exists()){
                file.mkdir()
            }
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("./export/$search-Results.csv"), "UTF-8"))
            currentSearch.each {
                String row = "$it.id$CSV_SEPARATOR$it.scraper_id$CSV_SEPARATOR$it.type$CSV_SEPARATOR${it.result.replaceAll("\n", " ")}$CSV_SEPARATOR${it.getDateAsString() != null ? it.getDateAsString() : 'null'}$CSV_SEPARATOR$it.source$CSV_SEPARATOR$it.category\n"
                bw.write(row)
            }
            bw.flush()
            bw.close()
        }
        catch (UnsupportedEncodingException e) {
            logger.warn("Error while exporting csv. (UnsupportedEncodingException)")
            e.printStackTrace()
        }
        catch (FileNotFoundException e){
            logger.warn("Error while exporting csv. (FileNotFoundException)")
            e.printStackTrace()
        }
        catch (IOException e){
            logger.warn("Error while exporting csv. (IOException)")
            e.printStackTrace()
        }
    }

    static searchDatabase(String search){
        currentSearch = db.getSearchResult(search)

        List<ScraperResult> rsSpiegel = db.getSearchResult(search, db.getScraperID("SpiegelOnline"))
        List<ScraperResult> rsWelt = db.getSearchResult(search, db.getScraperID("Die Welt"))
        List<ScraperResult> rsFocus = db.getSearchResult(search, db.getScraperID("Focus"))
        List<ScraperResult> rsBild = db.getSearchResult(search, db.getScraperID("Die Bild"))
        List<ScraperResult> rsSueddeutsche = db.getSearchResult(search, db.getScraperID("Süddeutsche"))

        resultArea.clear()
        resultArea.appendText("Anzahl Artikel \n")
        resultArea.appendText("SiegelOnline\t: ${rsSpiegel.size()} \n" )
        resultArea.appendText("Welt\t\t\t: ${rsWelt.size()} \n" )
        resultArea.appendText("Focus\t\t: ${rsFocus.size()} \n" )
        resultArea.appendText("Bild\t\t\t: ${rsBild.size()} \n" )
        resultArea.appendText("Süddeutsche\t: ${rsSueddeutsche.size()} \n" )

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
            stop = true
        }
    }
}
