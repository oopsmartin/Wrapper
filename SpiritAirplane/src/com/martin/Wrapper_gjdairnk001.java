package com.martin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.lang.StringUtils;

import com.qunar.qfwrapper.bean.booking.BookingInfo;
import com.qunar.qfwrapper.bean.booking.BookingResult;
import com.qunar.qfwrapper.bean.search.FlightDetail;
import com.qunar.qfwrapper.bean.search.FlightSearchParam;
import com.qunar.qfwrapper.bean.search.FlightSegement;
import com.qunar.qfwrapper.bean.search.OneWayFlightInfo;
import com.qunar.qfwrapper.bean.search.ProcessResultInfo;
import com.qunar.qfwrapper.constants.Constants;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.QFGetMethod;
import com.qunar.qfwrapper.util.QFHttpClient;
import com.qunar.qfwrapper.util.QFPostMethod;

/*
 *  Modified Information
 *  Author: martin.wang
 *  Date:   2014-09-05
 *  Time:   16:13
 */

public class Wrapper_gjdairnk001 implements QunarCrawler
{
    private static final String CODEBASE = "gjdairnk001";
    static FlightSearchParam searchParam = new FlightSearchParam();
    QFHttpClient httpClient = new QFHttpClient(searchParam, false);

    private static final String SEARCHURL = "https://www.spirit.com/Default.aspx?action=search";
    private static final String SELECTURL = "https://www.spirit.com/DPPCalendarMarket.aspx";
    private static final String SEARCHURLBYBOOKING = "http://www.spirit.com/Default.aspx?action=search";

    public static Map<String, String> monthMap = new HashMap<String, String>();
    static
    {
        monthMap.put("01", "january");
        monthMap.put("02", "february");
        monthMap.put("03", "march");
        monthMap.put("04", "april");
        monthMap.put("05", "may");
        monthMap.put("06", "june");
        monthMap.put("07", "july");
        monthMap.put("08", "august");
        monthMap.put("09", "september");
        monthMap.put("10", "october");
        monthMap.put("11", "november");
        monthMap.put("12", "december");
    }

    public String getHtml(FlightSearchParam flightSearchParam)
    {
        httpClient = new QFHttpClient(flightSearchParam, false);
        httpClient.getParams().setCookiePolicy(
                CookiePolicy.BROWSER_COMPATIBILITY);
        QFPostMethod post = null;
        QFGetMethod get = null;

        String dep = flightSearchParam.getDep();
        String arr = flightSearchParam.getArr();
        String[] depts = flightSearchParam.getDepDate().split("-");
        String month = monthMap.get(depts[1]);
        String depDate = month + " " + depts[2] + ", " + depts[0];
        String depd = month + " " + depts[2];

        NameValuePair[] nameValuePairs = {
                new NameValuePair("birthdates", ""),
                new NameValuePair("lapoption", ""),
                new NameValuePair("awardFSNumber", ""),
                new NameValuePair("bookingType", "F"),
                new NameValuePair("hotelOnlyInput", ""),
                new NameValuePair("autoCompleteValueHidden", ""),
                new NameValuePair("from", dep), //
                new NameValuePair("to", arr), //
                new NameValuePair("tripType", "oneWay"),
                new NameValuePair("departDate", depDate), //
                new NameValuePair("departDateDisplay", depd), //
                new NameValuePair("returnDate", ""),
                new NameValuePair("returnDateDisplay", ""),
                new NameValuePair("carPickUpTime", "16"),
                new NameValuePair("carDropOffTime", "16"),
                new NameValuePair("ADT", "1"), new NameValuePair("CHD", "0"),
                new NameValuePair("INF", "0"),
                new NameValuePair("promoCode", "") };

        try
        {
            post = new QFPostMethod(SEARCHURL);
            post.setRequestHeader("Host", "www.spirit.com");
            post.setRequestHeader("Referer",
                    "https://www.spirit.com/Default.aspx?culture=en-US");
            post.addParameters(nameValuePairs);
            httpClient.executeMethod(post);

            get = new QFGetMethod(SELECTURL);
            get.setRequestHeader("Host", "www.spirit.com");
            get.setRequestHeader("Referer",
                    "http://www.spirit.com/Default.aspx");
            httpClient.executeMethod(get);
            return get.getResponseBodyAsString();
        } catch (Exception e)
        {
            return "Exception";
        } finally
        {
            if (post != null)
            {
                post.releaseConnection();
            }
            if (get != null)
            {
                get.releaseConnection();
            }
        }
    }

    public ProcessResultInfo process(String html,
            FlightSearchParam flightSearchParam)
    {
        ProcessResultInfo processResultInfo = new ProcessResultInfo();
        List<OneWayFlightInfo> data = new ArrayList<OneWayFlightInfo>();

        
        if (StringUtils.equals(html, "Exception"))
        {
            processResultInfo.setStatus(Constants.CONNECTION_FAIL);
            processResultInfo.setRet(false);
            processResultInfo.setData(data);
            return processResultInfo;
        }

        if (StringUtils.contains(html, "there are no seats available for"))
        {
            processResultInfo.setStatus(Constants.INVALID_DATE);
            processResultInfo.setRet(false);
            processResultInfo.setData(data);
            return processResultInfo;
        }

        printSymbol(html,"a.txt");
        try
        {
            List<OneWayFlightInfo> goList = new ArrayList<OneWayFlightInfo>();

            String currencyCode = StringUtils
                    .substringBetween(
                            html,
                            "<li><input type=\"radio\" name=\"radio_fare_btn1\" class=\"displayPriceToggle\" value=\"price\" onClick=\"javascript:SetValue(this);\">",
                            "<").trim();

            String[] bodys = StringUtils.substringsBetween(html,
                    "<tr class=\"rowsMarket1\"", "<td class=\"fare\"");

            for (String body : bodys)
            {
                OneWayFlightInfo oneflightInfo = new OneWayFlightInfo();
                List<FlightSegement> segs = new ArrayList<FlightSegement>();
                FlightDetail flightDetail = new FlightDetail();
                List<String> flightNoList = new ArrayList<String>();

                String rawTotalPrice = StringUtils.substringBetween(body,
                        "<em class=\"emPrice\">$", "</em></label></div>");
                String totalPrice = rawTotalPrice;
                if(rawTotalPrice.contains("emPrice"))
                    totalPrice = rawTotalPrice.substring(rawTotalPrice.indexOf("<em class=\"emPrice\">$")+"<em class=\"emPrice\">$".length());
                System.out.printf("total price  is %s\n",totalPrice);
                String basePrice = StringUtils.substringBetween(body, "class=\"standardPrice\"><span data-price=\"", "\">");
                
                System.out.printf("price is %s\n",basePrice);
                
                double price = Double.valueOf(basePrice);
                double tprice = Double.valueOf(totalPrice);
                double tax = tprice - price;
                String wrapperID = String.valueOf(this.getClass()).substring(
                        String.valueOf(this.getClass()).indexOf('_') + 1);
                BigDecimal b = new BigDecimal(tax);
                tax = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                String outflightInfo = StringUtils.substringBetween(body,
                        "<input id=\"DLXRadio", "<em class=\"emPrice\">");

                outflightInfo = StringUtils.substringBetween(outflightInfo,
                        "value=\"", "\">");
                outflightInfo = outflightInfo.split("\\|")[1];

                String[] outflightInfos = outflightInfo.split("\\^");

                for (int i = 0; i < outflightInfos.length; i++)
                {
                    FlightSegement seg = new FlightSegement();
                    String[] infos = outflightInfos[i].split("~");
                    String exCode = infos[0].trim();
                    String codeNum = infos[1].trim();
                    String code = exCode + codeNum;
                    String dep = infos[4].trim();
                    String depTimeInfo = infos[5].trim();
                    String depDate = depTimeInfo.split(" ")[0];
                    String[] depDates = depDate.split("/");
                    depDate = depDates[2] + "-" + depDates[0] + "-"
                            + depDates[1];
                    String depTime = depTimeInfo.split(" ")[1];
                    String arr = infos[6].trim();
                    String arrTimeInfo = infos[7].trim();
                    String arrDate = arrTimeInfo.split(" ")[0];
                    String[] arrDates = arrDate.split("/");
                    arrDate = arrDates[2] + "-" + arrDates[0] + "-"
                            + arrDates[1];
                    String arrTime = arrTimeInfo.split(" ")[1];

                    flightNoList.add(code);
                    seg.setArrairport(arr);
                    seg.setArrDate(arrDate);
                    seg.setArrtime(arrTime);
                    seg.setDepairport(dep);
                    seg.setDepDate(depDate);
                    seg.setDeptime(depTime);
                    seg.setFlightno(code);

                    segs.add(seg);
                }

                flightDetail.setFlightno(flightNoList);
                flightDetail.setMonetaryunit(currencyCode);
                flightDetail.setPrice(price);
                flightDetail.setTax(tax);
                flightDetail.setDepdate(Date.valueOf(flightSearchParam
                        .getDepDate()));
                flightDetail.setDepcity(flightSearchParam.getDep());
                flightDetail.setArrcity(flightSearchParam.getArr());
                flightDetail.setWrapperid(wrapperID);
                oneflightInfo.setDetail(flightDetail);
                oneflightInfo.setInfo(segs);
                goList.add(oneflightInfo);

            }

            if (null != goList && goList.size() > 0)
            {
                data.addAll(goList);
            } else
            {
                processResultInfo.setStatus(Constants.NO_RESULT);
                processResultInfo.setRet(false);
                processResultInfo.setData(data);
                return processResultInfo;
            }

            processResultInfo.setStatus(Constants.SUCCESS);
            processResultInfo.setRet(true);
            processResultInfo.setData(data);
            return processResultInfo;

        } catch (Exception e)
        {
            processResultInfo.setStatus(Constants.PARSING_FAIL);
            processResultInfo.setRet(false);
            processResultInfo.setData(data);
            return processResultInfo;
        }
    }

    public BookingResult getBookingInfo(FlightSearchParam flightSearchParam)
    {
        BookingResult bookingResult = new BookingResult();
        String dep = flightSearchParam.getDep();
        String arr = flightSearchParam.getArr();
        BookingInfo bookingInfo = new BookingInfo();
        bookingInfo.setAction(SEARCHURLBYBOOKING);
        bookingInfo.setContentType("UTF-8");
        bookingInfo.setMethod("post");
        java.util.Map<String, String> inputs = new HashMap<String, String>();

        String[] depts = flightSearchParam.getDepDate().split("-");
        String month = monthMap.get(depts[1]);
        String depDate = month + " " + depts[2] + ", " + depts[0];
        String depd = month + " " + depts[2];

        inputs.put("birthdates", "");
        inputs.put("lapoption", "");
        inputs.put("awardFSNumber", "");
        inputs.put("bookingType", "F");
        inputs.put("hotelOnlyInput", "");
        inputs.put("autoCompleteValueHidden", "");
        inputs.put("from", dep);
        inputs.put("to", arr);
        inputs.put("tripType", "oneWay");
        inputs.put("departDate", depDate);
        inputs.put("departDateDisplay", depd);
        inputs.put("returnDate", depDate);
        inputs.put("returnDateDisplay", "");
        inputs.put("carPickUpTime", "16");
        inputs.put("carDropOffTime", "16");
        inputs.put("ADT", "1");
        inputs.put("CHD", "0");
        inputs.put("INF", "0");
        inputs.put("promoCode", "");

        bookingInfo.setInputs(inputs);
        bookingResult.setRet(true);
        bookingResult.setData(bookingInfo);
        return bookingResult;
    }

    public static void main(String[] args)
    {

        FlightSearchParam param = new FlightSearchParam();
        param.setDep("BWI");
        param.setArr("LAX");
        param.setDepDate("2014-10-15");
        param.setWrapperid("gjdairnk001");
        Wrapper_gjdairnk001 gjdair3u = new Wrapper_gjdairnk001();
        String html = gjdair3u.getHtml(param);
        
        ProcessResultInfo result = gjdair3u.process(html, param);
        if (result.isRet() && result.getStatus().equals(Constants.SUCCESS)) {
            List<OneWayFlightInfo> flightList = (List<OneWayFlightInfo>) result.getData();
            for (OneWayFlightInfo in : flightList) {
                System.out.println("************" + in.getInfo().toString());
                System.out.println("++++++++++++" + in.getDetail().toString());
            }
        } else
            System.out.println(result.getStatus());

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
