package com.martin.philippine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import com.google.common.collect.Lists;
import com.qunar.qfwrapper.bean.booking.BookingInfo;
import com.qunar.qfwrapper.bean.booking.BookingResult;
import com.qunar.qfwrapper.bean.search.FlightDetail;
import com.qunar.qfwrapper.bean.search.FlightSearchParam;
import com.qunar.qfwrapper.bean.search.FlightSegement;
import com.qunar.qfwrapper.bean.search.ProcessResultInfo;
import com.qunar.qfwrapper.bean.search.RoundTripFlightInfo;
import com.qunar.qfwrapper.constants.Constants;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.QFGetMethod;
import com.qunar.qfwrapper.util.QFHttpClient;
import com.qunar.qfwrapper.util.QFPostMethod;

/**
 * Created with IntelliJ IDEA. User: ling.zhao Date: 14-6-7 Time: 下午1:45 To change this template use File | Settings |
 * File Templates.
 * 
 * updated Author: martin.wang Date: 2014-09-03 Time: 11:00
 */
public class Wrapper_gjsairpr001 implements QunarCrawler {

    public static void main(String[] args) {
        FlightSearchParam param = new FlightSearchParam();
        param.setDep("PVG");
        param.setArr("BKK");
        param.setDepDate("2014-11-24");
        param.setWrapperid("gjsairpr001");
        param.setRetDate("2014-11-28");
        Wrapper_gjsairpr001 gjdair3u = new Wrapper_gjsairpr001();
        String html = gjdair3u.getHtml(param);
        ProcessResultInfo result = gjdair3u.process(html, param);
        if (result.isRet() && result.getStatus().equals(Constants.SUCCESS)) {
            List<RoundTripFlightInfo> flightList = (List<RoundTripFlightInfo>) result.getData();
            for (RoundTripFlightInfo in : flightList) {
                System.out.println("************" + in.getInfo().toString());
                System.out.println("************" + in.getRetinfo().toString());
                System.out.println("++++++++++++" + in.getDetail().toString());
                System.out.println("++++++++++++" + in.getRetflightno());
            }
        } else
            System.out.println(result.getStatus());
    }

    public String getHtml(FlightSearchParam param) {
        QFHttpClient httpClient = new QFHttpClient(param, false);
        String cookieStr = "";
        httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        String dates[] = param.getDepDate().split("-");
        String redates[] = param.getRetDate().split("-");
        PostMethod postMethod = new QFPostMethod(
                "https://onlinebooking.philippineairlines.com/flypal/AirLowFareSearchExternal.do");
        GetMethod getMethod = new QFGetMethod(
                "https://onlinebooking.philippineairlines.com/flypal/AirFareFamiliesForward.do");
        try {
            String body = "tripType=RT&outboundOption.originLocationCode="
                    + param.getDep()
                    + "&outboundOption.originLocationName=&inboundOption.originLocationCode="
                    + "&inboundOption.originLocationName=&outboundOption.destinationLocationName=&inboundOption.destinationLocationCode="
                    + "&inboundOption.destinationLocationName="
                    + "&outboundOption1="
                    + dates[1]
                    + "/"
                    + dates[2]
                    + "/"
                    + dates[0]
                    + "&outboundOption.departureDate="
                    + dates[1]
                    + "/"
                    + dates[2]
                    + "/"
                    + dates[0]
                    + "&outboundOption.departureMonth="
                    + dates[1]
                    + "&outboundOption.departureDay="
                    + dates[2]
                    + "&outboundOption.departureYear="
                    + dates[0]
                    + "&inboundOption1="
                    + redates[1]
                    + "/"
                    + redates[2]
                    + "/"
                    + redates[0]
                    + "&inboundOption.departureDate="
                    + redates[1]
                    + "/"
                    + redates[2]
                    + "/"
                    + redates[0]
                    + "&inboundOption.departureDate="
                    + redates[1]
                    + "/"
                    + redates[2]
                    + "/"
                    + redates[0]
                    + "&inboundOption.departureMonth="
                    + redates[1]
                    + "&inboundOption.departureDay="
                    + redates[2]
                    + "&inboundOption.departureYear="
                    + redates[0]
                    + "&outboundOption.destinationLocationCode="
                    + param.getArr()
                    + "&guestTypes%5B0%5D.amount=1&guestTypes%5B0%5D.type=ADT&guestTypes%5B1%5D.amount=0&guestTypes%5B1%5D.type=CNN&guestTypes%5B2%5D.amount=0&guestTypes%5B2%5D.type=INS&guestTypes%5B3%5D.amount=0&guestTypes%5B3%5D.type=INF&flexibleSearch=false&coupon=";

            postMethod.setRequestEntity(new StringRequestEntity(body, "application/x-www-form-urlencoded", null));
            postMethod.addRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            String postHtml = "";
            httpClient.executeMethod(postMethod);
            cookieStr = StringUtils.join(httpClient.getState().getCookies(), "; ");
            postMethod = new QFPostMethod("https://onlinebooking.philippineairlines.com/flypal/AirLowFareSearchExt.do");
            body = "ajaxAction=true";
            postMethod.setQueryString(body);
            postMethod.setRequestHeader("Cookie", cookieStr);
            httpClient.executeMethod(postMethod);
            postHtml = postMethod.getResponseBodyAsString();
            if (postHtml.contains("Seats may have been SOLD OUT or there are NO DIRECT FLIGHTS")) {
                return "invalidateDate";
            }
            cookieStr = org.apache.commons.lang.StringUtils.join(httpClient.getState().getCookies(), "; ");
            if (postHtml.contains("/flypal/AirFareFamiliesForward.do")) { /* direct flight */
                getMethod.setRequestHeader("Cookie", cookieStr);
                httpClient.executeMethod(getMethod);
                return getMethod.getResponseBodyAsString();
            } else { /* need transit */
                getMethod = new QFGetMethod(
                        "https://onlinebooking.philippineairlines.com/flypal/AirAvailabilitySearchForward.do");
                getMethod.setRequestHeader("Cookie", cookieStr);
                httpClient.executeMethod(getMethod);
                cookieStr = org.apache.commons.lang.StringUtils.join(httpClient.getState().getCookies(), "; ");
                String getHtml = getMethod.getResponseBodyAsString();
                if (getHtml.contains("There are no flights available on this date")) {
                    return "NO_RESULT";
                } else { // select the first comeFlight and the last goFlight
                    int inSelected = 0;
                    String div = StringUtils.substringBetween(getHtml, "id=\"AirFlightSelectForm\">",
                            "<div class=\"spacerVert\">");
                    String flights[] = div.split("flightHeading");
                    if (flights.length != 3) {
                        return "NO_RESULT";
                    }
                    String goFlights[] = flights[2].split("blockSeparator blockSep1");
                    inSelected = goFlights.length - 1;
                    postMethod = new QFPostMethod("https://onlinebooking.philippineairlines.com/flypal/AirPrice.do");
                    postMethod.setRequestHeader("Cookie", cookieStr);
                    postMethod.setQueryString("ajaxAction=true&flightId%5B0%5D=0&selected0=0&flightId%5B1%5D="
                            + inSelected + "&selected1=" + inSelected + "&vsessionid=");
                    httpClient.executeMethod(postMethod);
                    cookieStr = org.apache.commons.lang.StringUtils.join(httpClient.getState().getCookies(), "; ");
                    getMethod = new QFGetMethod(
                            "https://onlinebooking.philippineairlines.com/flypal/ItinerarySummary.do");
                    getMethod.setRequestHeader("Cookie", cookieStr);
                    httpClient.executeMethod(getMethod);
                    return getMethod.getResponseBodyAsString();
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
            if (getMethod != null) {
                getMethod.releaseConnection();
            }
        }
        return "Exception";
    }

    public ProcessResultInfo process(String html, FlightSearchParam param) {

        ProcessResultInfo processResultInfo = new ProcessResultInfo();
        List<RoundTripFlightInfo> flightList = Lists.newArrayList();

        if ("Exception".equals(html)) {
            System.out.println("Exception");
            processResultInfo.setStatus(Constants.CONNECTION_FAIL);
            processResultInfo.setRet(false);
            return processResultInfo;
        }
        if ("invalidateDate".equals(html)) {
            processResultInfo.setStatus(Constants.INVALID_DATE);
            processResultInfo.setRet(false);
            return processResultInfo;
        }
        if (html.contains("<title>Your session has expired</title>")) {
            System.out.println("session expired");
            processResultInfo.setStatus(Constants.INVALID_DATE);
            processResultInfo.setRet(false);
            return processResultInfo;
        }

        // printSymbol(html,"a.txt");
        printSymbol(html, "b.txt");

        if ("NO_RESULT".equals(html) || html.length() < 300) {
            System.out.println("no result");
            processResultInfo.setStatus(Constants.NO_RESULT);
            processResultInfo.setRet(true);
            return processResultInfo;
        }
        try {
            String tables[] = StringUtils.substringsBetween(html, "<div class=\"flightSelectionByAirline",
                    "<div class=\"footerBlock\">");
            if (tables.length == 0) {
                System.out.println("no result");
                processResultInfo.setStatus(Constants.NO_RESULT);
                processResultInfo.setRet(true);
                return processResultInfo;
            }

            String cur = StringUtils.substringBetween(tables[0], "Prices shown are in", "and").trim();
            String wrapperID = String.valueOf(this.getClass()).substring(
                    String.valueOf(this.getClass()).indexOf('_') + 1);
            String rawTax = html.substring(html.indexOf("<div>Taxes, Fees and Charges")
                    + "<div>Taxes, Fees and Charges".length());
            // System.out.printf("rawTax is %s\n",rawTax);
            String tax = StringUtils.substringBetween(rawTax, "<div>", "</div>").trim();
            String goFlightsInfo = StringUtils.substringBetween(tables[0], "<tr class", "</tbody>").replaceAll("\\s",
                    "");
            String comeFlightsInfo = StringUtils.substringBetween(tables[1], "<tr class", "</tbody>").replaceAll("\\s",
                    "");

            String splitStr = "";
            // transfer flight
            if (goFlightsInfo.contains("combineRows")) {
                // combineRows represents for each individual flight ie frows[k]
                splitStr = "combineRows";
            }
            // direct flight
            else
                splitStr = "<tr class=\"";
            String goFrows[] = goFlightsInfo.split(splitStr);
            String comeFrows[] = comeFlightsInfo.split(splitStr);
            if (goFrows.length == 0) {
                processResultInfo.setStatus(Constants.NO_RESULT);
                processResultInfo.setRet(false);
                return processResultInfo;
            }

            // List<FlightSegement> goSegs = new ArrayList<FlightSegement>();
            // List<FlightSegement> comeSegs = new ArrayList<FlightSegement>();
            List<List<FlightSegement>> goSegs = new ArrayList<List<FlightSegement>>();
            List<List<FlightSegement>> comeSegs = new ArrayList<List<FlightSegement>>();
            List<List<String>> goFrowsNoList = new ArrayList<List<String>>();
            List<List<String>> comeFrowsNoList = new ArrayList<List<String>>();
            List<Double> goPriceList = new ArrayList<Double>();
            List<Double> comePriceList = new ArrayList<Double>();

            // go direction
            for (int i = 1; i < goFrows.length; i++) {
                // each frow is consisted of several connected rows represented by goFlight
                String goFlight[] = goFrows[i].split("colFlight");
                List<String> goFlightNoList = new ArrayList<String>();
                List<FlightSegement> goSeg = new ArrayList<FlightSegement>();

                for (int j = 1; j < goFlight.length; j++) {

                    FlightSegement flightSegement = new FlightSegement();
                    String flightNo = "pr0" + StringUtils.substringBetween(goFlight[j], "&flightNumber=", "&");
                    String goDepYear = StringUtils.substringBetween(goFlight[j], "&departureYear=", "&");
                    String goDepMonth = String.valueOf(Integer.valueOf(StringUtils.substringBetween(goFlight[j],
                            "&departureMonth=", "&")) + 1);
                    String goDepDay = StringUtils.substringBetween(goFlight[j], "&departureDay=", "&");
                    String goDepDate = goDepYear + "-" + (goDepMonth.length() == 1 ? "0" + goDepMonth : goDepMonth)
                            + "-" + (goDepDay.length() == 1 ? "0" + goDepDay : goDepDay);
                    String goDepTime = StringUtils.substringBetween(goFlight[j], "<tdclass=\"colDepart\"><div>",
                            "</div>").substring(0, 5);
                    String goArrTime = StringUtils.substringBetween(goFlight[j], "<tdclass=\"colArrive\"><div>",
                            "</div>").substring(0, 5);
                    String goAirports = StringUtils.substringBetween(goFlight[j], "colAirports\">", "</td>");
                    String goDepAirport = StringUtils
                            .substringBetween(goAirports, "style=\"display:none\">", "</span>").substring(0, 3);
                    String goArrAirport = StringUtils
                            .substringBetween(goAirports, "style=\"display:none\">", "</span>").substring(4, 7);
                    String goArrDate = goDepDate;
                    String rawGoPrice = StringUtils
                            .substringBetween(goFlight[j], "<divclass=\"colPrice\">", "</label>");
                    String goPrice = null;
                    if (rawGoPrice != null)
                        goPrice = (rawGoPrice.substring(rawGoPrice.indexOf(">") + 1)).replace(",", "");

                    flightSegement.setFlightno(flightNo);
                    flightSegement.setDepDate(goDepDate);
                    flightSegement.setDeptime(goDepTime);
                    flightSegement.setDepairport(goDepAirport);
                    flightSegement.setArrDate(goArrDate);
                    flightSegement.setArrtime(goArrTime);
                    flightSegement.setArrairport(goArrAirport);

                    goSeg.add(flightSegement);
                    goFlightNoList.add(flightNo);

                    if ((j - 1) % 2 == 0) {
                        goPriceList.add(Double.valueOf(goPrice));
                    }
                }// each goFlight piece
                goFrowsNoList.add(goFlightNoList);
                goSegs.add(goSeg);
            }// go frows

            // come direction
            for (int k = 1; k < comeFrows.length; k++) {
                String comeFlight[] = comeFrows[k].split("colFlight");
                List<String> comeFlightNoList = new ArrayList<String>();
                List<FlightSegement> comeSeg = new ArrayList<FlightSegement>();

                for (int l = 1; l < comeFlight.length; l++) {

                    FlightSegement flightSegement = new FlightSegement();
                    String flightNo = "pr0" + StringUtils.substringBetween(comeFlight[l], "&flightNumber=", "&");
                    String comeDepYear = StringUtils.substringBetween(comeFlight[l], "&departureYear=", "&");
                    String comeDepMonth = String.valueOf(Integer.valueOf(StringUtils.substringBetween(comeFlight[l],
                            "&departureMonth=", "&")) + 1);
                    String comeDepDay = StringUtils.substringBetween(comeFlight[l], "&departureDay=", "&");
                    String comeDepDate = comeDepYear + "-"
                            + (comeDepMonth.length() == 1 ? "0" + comeDepMonth : comeDepMonth) + "-"
                            + (comeDepDay.length() == 1 ? "0" + comeDepDay : comeDepDay);
                    String comeDepTime = StringUtils.substringBetween(comeFlight[l], "<tdclass=\"colDepart\"><div>",
                            "</div>").substring(0, 5);
                    String comeArrTime = StringUtils.substringBetween(comeFlight[l], "<tdclass=\"colArrive\"><div>",
                            "</div>").substring(0, 5);
                    String comeAirports = StringUtils.substringBetween(comeFlight[l], "colAirports\">", "</td>");
                    String comeDepAirport = StringUtils.substringBetween(comeAirports, "style=\"display:none\">",
                            "</span>").substring(0, 3);
                    String comeArrAirport = StringUtils.substringBetween(comeAirports, "style=\"display:none\">",
                            "</span>").substring(4, 7);
                    String comeArrDate = comeDepDate;
                    String rawComePrice = StringUtils.substringBetween(comeFlight[l], "<divclass=\"colPrice\">",
                            "</label>");
                    String comePrice = null;
                    if (rawComePrice != null)
                        comePrice = (rawComePrice.substring(rawComePrice.indexOf(">") + 1)).replace(",", "");
                    System.out.printf("comePrice is %s\n", comePrice);

                    flightSegement.setFlightno(flightNo);
                    flightSegement.setDepDate(comeDepDate);
                    flightSegement.setDeptime(comeDepTime);
                    flightSegement.setDepairport(comeDepAirport);
                    flightSegement.setArrDate(comeArrDate);
                    flightSegement.setArrtime(comeArrTime);
                    flightSegement.setArrairport(comeArrAirport);

                    comeSeg.add(flightSegement);
                    comeFlightNoList.add(flightNo);

                    if ((l - 1) % 2 == 0) {
                        comePriceList.add(Double.valueOf(comePrice));
                    }
                }// each comeFlight piece
                comeFrowsNoList.add(comeFlightNoList);
                comeSegs.add(comeSeg);
            }// come frows

            System.out.println("permutation");
            // permutation with goFrows and comeFrows
            for (int i = 1; i < goFrows.length; i++) {
                FlightDetail flightDetail = new FlightDetail();
                flightDetail.setArrcity(param.getArr());
                flightDetail.setDepcity(param.getDep());
                flightDetail.setDepdate(Date.valueOf(param.getDepDate()));
                flightDetail.setFlightno(goFrowsNoList.get(i - 1));
                flightDetail.setMonetaryunit(cur);
                flightDetail.setTax(Double.valueOf(tax));
                flightDetail.setWrapperid(wrapperID);

                for (int j = 1; j < comeFrows.length; j++) {
                    RoundTripFlightInfo roundInfo = new RoundTripFlightInfo();
                    flightDetail.setPrice(goPriceList.get((i - 1) / 2) + comePriceList.get((j - 1) / 2));
                    roundInfo.setInfo(goSegs.get(i - 1));
                    roundInfo.setRetdepdate(Date.valueOf(comeSegs.get(j - 1).get(0).getDepDate()));
                    roundInfo.setRetflightno(comeFrowsNoList.get(j - 1));
                    roundInfo.setRetinfo(comeSegs.get(j - 1));
                    roundInfo.setOutboundPrice(goPriceList.get((i - 1) / 2));
                    roundInfo.setReturnedPrice(goPriceList.get((i - 1) / 2) + comePriceList.get((j - 1) / 2));                    
                    roundInfo.setDetail(flightDetail);
                    flightList.add(roundInfo);
                }
            }// permutations
            processResultInfo.setData(flightList);

            System.out.println("setted data");
            processResultInfo.setRet(true);
            processResultInfo.setStatus(Constants.SUCCESS);
            return processResultInfo;

        } catch (Exception e) {
            System.out.println("exception");
            processResultInfo.setStatus(Constants.PARSING_FAIL);
            processResultInfo.setRet(false);
            return processResultInfo;
        }

    }

    public BookingResult getBookingInfo(FlightSearchParam param) {

        String[] depDates = param.getDepDate().toString().split("-");
        String[] arrDates = param.getRetDate().toString().split("-");
        BookingResult bookingResult = new BookingResult();
        BookingInfo bookingInfo = new BookingInfo();
        bookingInfo.setAction("https://onlinebooking.philippineairlines.com/flypal/AirLowFareSearchExternal.do");
        bookingInfo.setMethod("post");
        Map<String, String> inputs = new HashMap<String, String>();
        inputs.put("tripType", "RT");
        inputs.put("formType", "0");
        inputs.put("outboundOption.originLocationCode", param.getDep());
        inputs.put("outboundOption.destinationLocationCode", param.getArr());
        inputs.put("outboundOption.originLocationName", "");
        inputs.put("inboundOption.originLocationCode", param.getArr());
        inputs.put("inboundOption.originLocationName", "");
        inputs.put("outboundOption.destinationLocationName", "");
        inputs.put("inboundOption.destinationLocationCode", param.getDep());
        inputs.put("inboundOption.destinationLocationName", "");
        inputs.put("outboundOption.departureDay", depDates[2]);
        inputs.put("outboundOption.departureMonth", depDates[1]);
        inputs.put("outboundOption.departureYear", depDates[0]);
        inputs.put("outboundOption.departureTime", "NA");
        inputs.put("inboundOption.departureDay", arrDates[2]);
        inputs.put("inboundOption.departureMonth", arrDates[1]);
        inputs.put("inboundOption.departureYear", arrDates[0]);
        inputs.put("inboundOption.departureTime", "NA");
        inputs.put("flexibleSearch", "false");
        inputs.put("classOfTravelEx", "ECON_FIESTA_1.FAR");
        inputs.put("directFlightsOnly", "false");
        inputs.put("guestTypes[0].amount", "1");
        inputs.put("guestTypes[0].type", "ADT");
        inputs.put("guestTypes[1].amount", "0");
        inputs.put("guestTypes[1].type", "CNN");
        inputs.put("guestTypes[2].amount", "0");
        inputs.put("guestTypes[2].type", "INS");
        inputs.put("guestTypes[3].amount", "0");
        inputs.put("guestTypes[3].type", "INF");
        inputs.put("preferredAirline[0]", "");
        inputs.put("preferredAirline[1]", "");
        inputs.put("preferredAirline[2]", "");
        inputs.put("searchType", "FARE");
        inputs.put("cabinClass", "FIESTAPLUS");
        inputs.put("fareOptions", "1.FAR");
        inputs.put("validateAction", "AirLowFareSearch");
        bookingInfo.setInputs(inputs);
        bookingResult.setRet(true);
        bookingResult.setData(bookingInfo);
        return bookingResult;
    }

    public String getValue(String source, String regEx) {
        Matcher mm = Pattern.compile(regEx).matcher(source);
        return mm.find() ? mm.group(mm.groupCount() > 0 ? 1 : 0) : "";
    }

    public static String[] getValues(String source, String regEx) {
        Vector<String> vec = new Vector<String>(5);
        Matcher mm = Pattern.compile(regEx).matcher(source);
        while (mm.find()) {
            vec.add(mm.group(mm.groupCount() > 0 ? 1 : 0));
        }
        return vec.toArray(new String[0]);
    }

    private void printSymbol(String arg, String fileName) {
        try {
            FileWriter fw = null;
            File f = new File(fileName);

            if (!f.exists()) {
                f.createNewFile();
            }
            fw = new FileWriter(f);
            BufferedWriter out = new BufferedWriter(fw);
            out.write(arg, 0, arg.length() - 1);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
