import org.apache.commons.codec.digest.DigestUtils

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.text.SimpleDateFormat

/**
 * @author Dominik Elflein
 */
class DatabaseController {

    Log log = NewsScraperMain.logger

    final String CONNECTION_STRING = "jdbc:h2:./database/ScraperData"

    Connection conn = null

    boolean locked = false

    /**
     * Creates the Database Controller
     * -> Create Table for Scrapers
     * -> Create Table for Results
     */
    DatabaseController(){
        try{
        Class.forName("org.h2.Driver")
        getConnection()

        Statement stm = conn.createStatement()

        stm.executeUpdate("CREATE TABLE IF NOT EXISTS scraper(ID VARCHAR(32) PRIMARY KEY NOT NULL, name VARCHAR(255))")
        stm.executeUpdate("CREATE TABLE IF NOT EXISTS scraper_results(ID VARCHAR(32) PRIMARY KEY NOT NULL, scraper_id VARCHAR(255) NOT NULL , type VARCHAR(64), result LONGTEXT, date DATE, source VARCHAR(255), category VARCHAR(64))")

        }catch (SQLException e){
            e.printStackTrace()
            log.error("Failed to initialize Database.")
        }finally{
            if (conn != null) conn.close()
            locked = false
        }
    }

    /**
     * Opens the Database Connection, after it got closed to save Resources
     *
     */
    private def getConnection(){
        while(locked){
            sleep(500)
        }
        conn = DriverManager.getConnection(CONNECTION_STRING)
        locked = true
    }

    /**
     * If a new Scraper is found, and not yet mapped to the Database, this function will handle this.
     * -> Generate Scraper ID from scraper Name (MD5)
     * -> Insert into Database
     * -> Enable Scraper to be executed
     *
     * @param scraper - Scraper that is Child of ScraperJob
     */
    def createScraper(ScraperJob scraper){
        try{
            getConnection()
            Statement stm = conn.createStatement()

            String id = DigestUtils.md5Hex(scraper.scraperName)

            stm.executeUpdate("INSERT INTO scraper VALUES('$id', '$scraper.scraperName')")
            log.info("Generated ID for $scraper.scraperName ID: $id")
            scraper.enable()
        }catch(SQLException e){
            e.printStackTrace()
            log.error("Failed to create Scraper ${scraper.scraperName} on database")
        }finally{
            if(conn!= null) conn.close()
            locked = false
        }
    }

    /**
     * Get the ID of a Scraper by its Name. This is used to load the ID into memory for further Use.
     *
     * @param name
     * @return ID of the Scraper
     */
    String getScraperID(String name){
        try{
            getConnection()
            Statement stm = conn.createStatement()
            ResultSet rs = stm.executeQuery("SELECT * FROM scraper WHERE name LIKE '%$name%'")
            if(rs.next()){
                return rs.getString("ID")
            }else{
                return null
            }
        }catch(SQLException e){
            e.printStackTrace()
            log.error("Failed to get ScraperID for $name")
            return null
        }finally{
            if (conn != null) conn.close()
            locked = false
        }
    }

    /**
     * Returns true if there is already a Result with the idHash.
     *
     * @param idHash
     * @return true if idHash is in Database
     */
    boolean isExisting(String idHash){
        try{
            getConnection()

            Statement stm = conn.createStatement()
            ResultSet rs = stm.executeQuery("SELECT * FROM SCRAPER_RESULTS WHERE ID LIKE '$idHash'")
            if(rs.next()){
                return idHash == rs.getString("ID")
            }else{
                return false
            }

        }catch(SQLException e){
            e.printStackTrace()
            log.error("Failed to get Result for $idHash")
            return false
        }finally{
            if (conn != null) conn.close()
            locked = false
        }
    }

    /**
     * Adds a Text Result object to the Database.
     * -> Generate ID for Result by Hashing the first 60 chars (MD5)
     * -> To be a valid String it has to be cleared of some special Characters.
     * -> Check if already exists
     * -> Insert Result into Database
     *
     * Info: The date is the date of the sourcefile, not the current Date.
     *
     * @param id -> Id of the Scraper
     * @param text -> Resulttext
     * @param date -> Date of Source
     * @param source -> weblink to source
     */
    def addTextResult(String id, String text, Date date, String source, String category = null){
        try{
            String keyChain = text.size() > 60 ? text.substring(0,60) : text

            String idHash = DigestUtils.md5Hex(keyChain)

            text = text.replaceAll("\"", "")
            text = text.replaceAll("\'", "")
            text = text.replaceAll(";", "")
            text = text.replaceAll(":", "")
            text = text.replaceAll("\\(", "")
            text = text.replaceAll("\\)", "")
            text = text.replaceAll("\\[", "")
            text = text.replaceAll("]", "")
            text = text.replaceAll(",", "")
            text = text.replaceAll("\\.", "")

            if(!isExisting(idHash)){
                getConnection()
                Statement stm = conn.createStatement()

                if(date!=null){
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
                    String dateString = sdf.format(date)
                    stm.executeUpdate("INSERT INTO scraper_results VALUES('$idHash', '$id', 'TEXT', '$text', '$dateString', '$source', '$category')")
                }else{
                    stm.executeUpdate("INSERT INTO scraper_results VALUES('$idHash', '$id', 'TEXT', '$text', NULL, '$source', '$category')")
                }


                log.debug("Added '${text.size() > 60 ? text.substring(0,60) : text}' with ID:'$idHash' to database")
            }else{
                log.warn("'${text.size() > 60 ? text.substring(0,60) : text}' with ID:'$idHash' already exists.")
            }
        }catch(SQLException e){
            e.printStackTrace()
            log.error("Failed to add '${text.size() > 60 ? text.substring(0,60) : text}' to database.")
        }finally{
            if (conn != null) conn.close()
            locked = false
        }
    }

    /**
     * Adds an ImageLink to the Database.
     *
     * Full qualified Link contains <protocol>://<Base_URL>/
     *
     * @param id -> scraper ID
     * @param webImageLink -> Full qualified link to image
     * @param date -> Date of sourcefile
     * @param source -> Link to source
     */
    def addWebImageResult(String id, String webImageLink, Date date, String source, String category = null){
        try{
            String keyChain = webImageLink.size() > 60 ? webImageLink.substring(0,60) : webImageLink
            String idHash = DigestUtils.md5Hex(keyChain)

            if(!isExisting(idHash)){
                getConnection()
                Statement stm = conn.createStatement()

                if(date!=null){
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
                    String dateString = sdf.format(date)
                    stm.executeUpdate("INSERT INTO scraper_results VALUES('$idHash', '$id', 'WEBIMAGE', '$webImageLink', '$dateString', '$source', '$category')")
                }else{
                    stm.executeUpdate("INSERT INTO scraper_results VALUES('$idHash', '$id', 'WEBIMAGE', '$webImageLink', NULL, '$source', '$category')")
                }
                log.debug("Added '${webImageLink.size() > 60 ? webImageLink.substring(0,60) : webImageLink}' with ID:$id to database.")
            }else{
                log.debug("'${webImageLink.size() > 60 ? webImageLink.substring(0,60) : webImageLink}' with ID:'$idHash' already exists.")
            }
        }catch(SQLException e){
            e.printStackTrace()
            log.error("Failed to add '${webImageLink.size() > 60 ? webImageLink.substring(0,60) : webImageLink}' to database.")
        }finally{
            if (conn != null) conn.close()
            locked = false
        }
    }

}
