/**
 * Created by Domin on 28.06.2017.
 */

boolean stop = true
List<ScraperJob> avaiableScraper = [
        new BildScraper("Bild"),
        new FocusScraper("Focus"),
        new SpiegelScraper("Spiegel"),
        new SueddeutscheScraper("Sueddeutsche"),
        new WeltScraper("Welt")
]

init()
while(!stop){
    avaiableScraper.each {
        println("Launching $it.scraperName ...")
        it.runScraper()
    }
    println("Pausing program for 8 hours ...")
    long timeLimit = 8*3600*1000 // 8 Stunden
    long pastTime = 0L
    long warnTime = 30*60*1000 // 30 Minuten

    while(pastTime < timeLimit){
        sleep(warnTime)
        pastTime+= warnTime
        long leftTime = timeLimit - pastTime
        double timeHourLeft = (((leftTime/1000)/60)/60)
        println("$timeHourLeft, until next autoRun.")
    }
    println("Starting next autoRun.")
}


def init(){
    println("Preparing Autorun")
    if(NewsScraperMain.db == null){
        println("No Database Connection...")
    }
    stop = false
}
