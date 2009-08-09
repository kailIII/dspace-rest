/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.rest.entities;

import org.sakaiproject.entitybus.entityprovider.annotations.EntityId;
import org.sakaiproject.entitybus.entityprovider.annotations.EntityFieldRequired;
import org.sakaiproject.entitybus.entityprovider.annotations.EntityId;
import org.dspace.app.statistics.Report;
import org.dspace.app.statistics.Stat;
import org.dspace.app.statistics.Statistics;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.sql.SQLException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Hashtable;

/**
 *
 * @author Bojan Suzic, bojan.suzic@gmail.com
 * Based on StatisticsServlet from JSP subproject project
 */
public class StatsEntity {

   @EntityId private int id;
   HashMap generalStats = new HashMap<String, String>();


   // TODO inspect and add additional fields


   public StatsEntity(Context context) throws SQLException {

       try { 
            showStatistics(context); }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
   };


   public String toString() {
       return "id:" + this.id + ", stuff.....";
   }


    private void showStatistics(Context context)
        throws IOException, SQLException
    {
        StringBuffer report = new StringBuffer();
        String date = null;

        File reportDir = new File(ConfigurationManager.getProperty("report.dir"));

        File[] reports = reportDir.listFiles();
        File reportFile = null;

        FileInputStream fir = null;
        InputStreamReader ir = null;
        BufferedReader br = null;

        try
        {
            List monthsList = new ArrayList();

            Pattern monthly = Pattern.compile("report-([0-9][0-9][0-9][0-9]-[0-9]+)\\.html");
            Pattern general = Pattern.compile("report-general-([0-9]+-[0-9]+-[0-9]+)\\.html");

            // FIXME: this whole thing is horribly inflexible and needs serious
            // work; but as a basic proof of concept will suffice

            // if no date is passed then we want to get the most recent general
            // report
            if (date == null)
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'M'-'dd");
                Date mostRecentDate = null;

                for (int i = 0; i < reports.length; i++)
                {
                    Matcher matchGeneral = general.matcher(reports[i].getName());
                    if (matchGeneral.matches())
                    {
                        Date parsedDate = null;

                        try
                        {
                             parsedDate = sdf.parse(matchGeneral.group(1).trim());
                        }
                        catch (ParseException e)
                        {
                            // FIXME: currently no error handling
                        }

                        if (mostRecentDate == null)
                        {
                            mostRecentDate = parsedDate;
                            reportFile = reports[i];
                        }

                        if (parsedDate != null && parsedDate.compareTo(mostRecentDate) > 0)
                        {
                            mostRecentDate = parsedDate;
                            reportFile = reports[i];
                        }
                    }
                }
            }

            // if a date is passed then we want to get the file for that month
            if (date != null)
            {
                String desiredReport = "report-" + date + ".html";

                for (int i = 0; i < reports.length; i++)
                {
                    if (reports[i].getName().equals(desiredReport))
                    {
                        reportFile = reports[i];
                    }
                }
            }

            if (reportFile == null)
            {
              System.out.println(" blank stats ");
            }

            // finally, build the list of report dates
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'M");
            for (int i = 0; i < reports.length; i++)
            {
                Matcher matchReport = monthly.matcher(reports[i].getName());
                if (matchReport.matches())
                {
                    Date parsedDate = null;

                    try
                    {
                         parsedDate = sdf.parse(matchReport.group(1).trim());
                    }
                    catch (ParseException e)
                    {
                        // FIXME: currently no error handling
                    }

                    monthsList.add(parsedDate);
                }
            }

            Date[] months = new Date[monthsList.size()];
            months = (Date[]) monthsList.toArray(months);

            Arrays.sort(months);

            try
            {
                fir = new FileInputStream(reportFile.getPath());
                ir = new InputStreamReader(fir, "UTF-8");
                br = new BufferedReader(ir);
            }
            catch (IOException e)
            {
                // FIXME: no error handing yet
                throw new RuntimeException(e.getMessage(),e);
            }

            // FIXME: there's got to be a better way of doing this
            String line = null;
            while ((line = br.readLine()) != null)
            {
                report.append(line);
            }
        }
        finally
        {
            if (br != null)
                try { br.close(); } catch (IOException ioe) { }

            if (ir != null)
                try { ir.close(); } catch (IOException ioe) { }

            if (fir != null)
                try { fir.close(); } catch (IOException ioe) { }
        }
     
        String filteredReport = report.toString();
        filteredReport = filteredReport.replaceAll("<div.*?>.*?</div>","");
        filteredReport = filteredReport.replaceAll("<style.*?>.*?</style>","");
        filteredReport = filteredReport.replaceAll("<th.*?>.*?</th>","");
        filteredReport = filteredReport.replaceAll("\t","");
        filteredReport = filteredReport.replaceAll("(<td.*?>)(.*?)(</td.*?>)(<td.*?>)(.*?)(</td.*?>)","$2::$5::");
        filteredReport = filteredReport.replaceAll("<.*?>","");
        System.out.println(filteredReport);
        String[] splittedReport = filteredReport.split("::");
        System.out.println(" :::::: " + splittedReport[0] + "=" + splittedReport[1]);
        for (int x=0; x<splittedReport.length; x++)
            generalStats.put(splittedReport[x], splittedReport[x+1]);

    }


    // FIXME: the methods here are written this way as the xml support is not
    // working for hashmaps in sakai completely

    public String getbrowse_mini() {
        return this.generalStats.get("browse_mini").toString();
    }

    public String getCommunityUpdates() {
        return this.generalStats.get("Community Updates").toString();
    }

    public String getWorkflowStarts() {
        return this.generalStats.get("Workflow Starts").toString();
    }

    public String getWarnings() {
        return this.generalStats.get("Warnings").toString();
    }

    public String getSubcommunitiesAdded() {
        return this.generalStats.get("Sub Community Added").toString();
    }

    public String getOAIRequests() {
        return this.generalStats.get("OAI Requests").toString();
    }

    public String getbrowse() {
        return this.generalStats.get("browse").toString();
    }

    public String getBitstreamViews() {
        return this.generalStats.get("Bitstream Views").toString();
    }

    public String getBitstreamupdates() {
        return this.generalStats.get("Bitstream Updates").toString();
    }

    public String getSearchesPerformed() {
        return this.generalStats.get("Searches Performed").toString();
    }

    public String getWorkSpaceItemViews() {
        return this.generalStats.get("Workspace Item Views").toString();
    }

    public String getBundlesCreated() {
        return this.generalStats.get("Bundles Created").toString();
    }

    public String getUserLogins() {
        return this.generalStats.get("User Logins").toString();
    }

    public String getCollectionViews() {
        return this.generalStats.get("Collection Views").toString();
    }

    public String getBundleUpdates() {
        return this.generalStats.get("Bundle Updates").toString();
    }

    public String getBitstreamsAdded() {
        return this.generalStats.get("Bitstreams Added").toString();
    }

    public String getItemViews() {
        return this.generalStats.get("Item Views").toString();
    }

    public String getItemsArchived() {
        return this.generalStats.get("Items Archived").toString();
    }

    public String getAllItems() {
        return this.generalStats.get("All Items").toString();
    }

    public String getCommunityviews() {
        return this.generalStats.get("Community Views").toString();
    }

    public String getUserHomePageViews() {
        return this.generalStats.get("User Home Page Views").toString();
    }

//    FIXME: this method will work later
//    public HashMap<String, String> getStats() {
//        return this.generalStats;
//    }

    

}