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
            new WeltScraper("Die Welt"),
            new BildScraper("Die Bild"),
            new FocusScraper("Focus"),
            new SueddeutscheScraper("Süddeutsche"),
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
