/**
 * @author Dominik Elflein
 */
class RegisterScraper {

    DatabaseController db = NewsScraperMain.db

    /**
     * All scrapers have to be registered here, to be visible in the GUI and for proper workflow.
     *
     */
    static List<ScraperJob> scrapers = [
            new SpiegelScraper("SpiegelOnline"),
    ]

    /**
     * Constructor which registers new Scrapers to Database and application.
     */
    RegisterScraper(){
        scrapers.each {scraper ->
            if(scraper.id == null){
                db.createScraper(scraper)
            }
        }
    }
}
