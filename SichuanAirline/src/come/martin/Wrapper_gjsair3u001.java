package come.martin;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.qunar.qfwrapper.bean.booking.BookingInfo;
import com.qunar.qfwrapper.bean.booking.BookingResult;
import com.qunar.qfwrapper.bean.search.*;
import com.qunar.qfwrapper.constants.Constants;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.QFHttpClient;
import com.qunar.qfwrapper.util.QFPostMethod;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: ling.zhao
 * Date: 14-6-6
 * Time: 下午5:38
 * To change this template use File | Settings | File Templates.
 */
public class Wrapper_gjsair3u001 implements QunarCrawler {
    private static final Logger logger = LoggerFactory.getLogger("WrapperException");
    private static final Logger ecLogger = LoggerFactory.getLogger("EC");

    public static void main(String[] args){
        FlightSearchParam param = new FlightSearchParam();        
        Wrapper_gjsair3u001 gjsair3u = new Wrapper_gjsair3u001();        
        param.setDep("HKG");
        param.setArr("CTU");
        param.setDepDate("2014-10-09");
        param.setWrapperid("gjsair3u001");
        param.setRetDate("2014-10-17");
        String html = gjsair3u.getHtml(param);
        ProcessResultInfo result = gjsair3u.process(html, param);
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
        /*
        System.out.println("result: " + JSONObject.toJSONString(processResultInfo));
        String booking = JSONObject.toJSONString(gjdair3u.getBookingInfo(param));
        System.out.println("booking: " + booking);
        */
    }

    @Override
    public String getHtml(FlightSearchParam param) {
        QFHttpClient httpClient = new QFHttpClient(param, false);
        QFPostMethod post = new QFPostMethod("http://www.scal.com.cn/B2C/ETicket/GetInterSingleChina");
        NameValuePair[] names = {
                new NameValuePair("RouteIndex","1"),
                new NameValuePair("RouteName",""),
                new NameValuePair("OrgCity",param.getDep()),
                new NameValuePair("DesCity",param.getArr()),
                new NameValuePair("OrgCityName",""),
                new NameValuePair("DesCityName",""),
                new NameValuePair("FlightDate",param.getDepDate()),
                new NameValuePair("SingleOrMore","1"),
                new NameValuePair("RFlightDate",param.getRetDate()),
                new NameValuePair("ROrgCity",param.getArr()),
                new NameValuePair("RDesCity",param.getDep()),
                new NameValuePair("IsRound","true"),
                new NameValuePair("PassKey",""),
                new NameValuePair("Flag","null"),
                new NameValuePair("BuyerType","0"),
                new NameValuePair("IsFixedCabin","false")
        };
        try {
            post.setRequestBody(names);
            post.setRequestHeader("Referer", "http://www.scal.com.cn/B2C/ETicket/InterAirlineList");
            post.getParams().setContentCharset("UTF-8");
            httpClient.executeMethod(post);
            String html = post.getResponseBodyAsString();
            if ("Exception".equalsIgnoreCase(html)){
                return "Exception";
            }
            if (html.contains("\"Result\":false")){
                return "NO_RESULT";
            }
            String info = org.apache.commons.lang.StringUtils.substringAfter(html, "\"AirlineListJSON\":\"").trim().replaceAll("}\"}", "}");
            if ("".equals(info)){
                return "NO_RESULT";
            }
            post = new QFPostMethod("http://www.scal.com.cn/B2C/ETicket/GetInterReturnPrice");
            info = info.replace("\\\"", "\"");
            String cabinsNo = StringUtils.substringBetween(info, "\"CurrentCabins\":{\"CabinsNO\":\"", "\"") .trim();
            NameValuePair[] names1 = {
                    new NameValuePair("CabinsNO",cabinsNo),
                    new NameValuePair("InterAirlineJSON",info),
                    new NameValuePair("IsMore","false")
            };
            post.setRequestBody(names1);
            post.getParams().setContentCharset("UTF-8");
            httpClient.executeMethod(post);
            return post.getResponseBodyAsString();
        } catch (Exception e) {
            logger.error("ErrorIn " + param.getWrapperid() + " : " + param.getWrapperid(), e);
        } finally {
            if (null != post){
                post.releaseConnection();
            }
        }
        return "Exception";
    }

    @Override
    public ProcessResultInfo process(String html, FlightSearchParam param) {
        ProcessResultInfo processResultInfo = new ProcessResultInfo();
        List<RoundTripFlightInfo> flightList = Lists.newArrayList();
        if ("Exception".equals(html)) {
            System.out.println("search_connection_fail_count");
            ecLogger.warn(param.getWrapperid() + ",OUT&IN," + Constants.CONNECTION_FAIL + "," + param.toString());
            processResultInfo.setStatus(Constants.CONNECTION_FAIL);
            processResultInfo.setRet(false);
            return processResultInfo;
        }
        if (html.contains("\"Result\":false")){
            System.out.println("search_no_result_count");
            ecLogger.warn(param.getWrapperid() + ",OUT&IN," + Constants.INVALID_DATE + ",NO FLIGHT," + param.toString());
            processResultInfo.setStatus(Constants.INVALID_DATE);
            processResultInfo.setRet(true);
            return processResultInfo;
        }
        try {
            String info = org.apache.commons.lang.StringUtils.substringAfter(html, "\"AirlineListJSON\":\"").trim().replaceAll("}\"}", "}");
            if (!"".equals(info) || null != info){
                RoundTripFlightInfo roundTripFlightInfo = new RoundTripFlightInfo();
                List<FlightSegement> outFsl = Lists.newArrayList();
                List<FlightSegement> retFsl = Lists.newArrayList();
                List<String> outFlightnos = Lists.newArrayList();
                List<String> retFlightnos = Lists.newArrayList();
                info = info.replace("\\\"", "\"");
                JSONObject jsonObject = JSONObject.parseObject(info);
                long totalTax = jsonObject.getLong("ADTotalTax");
                JSONObject outFlightMap = jsonObject.getJSONObject("FlightInfo");
                String outFlightNo = outFlightMap.getString("FlightNo");
                String outDept =  getValue(outFlightMap.getString("TakeOffTime"), "(\\d{2}:\\d{2}):\\d{2}");
                String outDepDate =  getValue(outFlightMap.getString("TakeOffTime"), "\\d{4}-\\d{2}-\\d{2}");
                String outArrt =  getValue(outFlightMap.getString("ArriveTime"), "(\\d{2}:\\d{2}):\\d{2}");
                String outArrDate =  getValue(outFlightMap.getString("ArriveTime"), "\\d{4}-\\d{2}-\\d{2}");
                //String outPlaneType = outFlightMap.getString("PlaneModel");
                JSONObject retFlightMap = jsonObject.getJSONObject("ReturnFlightInfo");
                String retFlightNo = retFlightMap.getString("FlightNo");
                String retDept =  getValue(retFlightMap.getString("TakeOffTime"), "(\\d{2}:\\d{2}):\\d{2}");
                String retDepDate =  getValue(retFlightMap.getString("TakeOffTime"), "\\d{4}-\\d{2}-\\d{2}");
                String retArrt =  getValue(retFlightMap.getString("ArriveTime"), "(\\d{2}:\\d{2}):\\d{2}");
                String retArrDate =  getValue(retFlightMap.getString("ArriveTime"), "\\d{4}-\\d{2}-\\d{2}");
                //String retPlaneType = retFlightMap.getString("PlaneModel");
                JSONObject currentCabins = retFlightMap.getJSONObject("CurrentCabins");
                long basePrice = currentCabins.getLong("ADBasePrice");
                String wrapperID = String.valueOf(this.getClass()).substring(
                        String.valueOf(this.getClass()).indexOf('_') + 1);
                FlightSegement outFlightSegment = new FlightSegement();
                outFlightSegment.setDepairport(param.getDep());
                outFlightSegment.setArrairport(param.getArr());
                outFlightSegment.setDepDate(outDepDate);
                outFlightSegment.setArrDate(outArrDate);
                outFlightSegment.setFlightno(outFlightNo);
                outFlightSegment.setDeptime(outDept);
                outFlightSegment.setArrtime(outArrt);
                outFlightnos.add(outFlightNo);
                outFsl.add(outFlightSegment);
                FlightSegement retFlightSegment = new FlightSegement();
                retFlightSegment.setDepairport(param.getArr());
                retFlightSegment.setArrairport(param.getDep());
                retFlightSegment.setDepDate(retDepDate);
                retFlightSegment.setArrDate(retArrDate);
                retFlightSegment.setFlightno(retFlightNo);
                retFlightSegment.setDeptime(retDept);
                retFlightSegment.setArrtime(retArrt);
                retFlightnos.add(retFlightNo);
                retFsl.add(retFlightSegment);
                FlightDetail flightDetail = new FlightDetail();
                flightDetail.setPrice(basePrice);
                flightDetail.setTax(totalTax);
                flightDetail.setDepdate(Date.valueOf(param.getDepDate()));
                flightDetail.setMonetaryunit("CNY");
                flightDetail.setFlightno(outFlightnos);
                flightDetail.setDepcity(param.getDep());
                flightDetail.setArrcity(param.getArr());
                flightDetail.setWrapperid(wrapperID);
                roundTripFlightInfo.setDetail(flightDetail);
                roundTripFlightInfo.setInfo(outFsl);
                roundTripFlightInfo.setRetdepdate(Date.valueOf(param.getRetDate()));
                roundTripFlightInfo.setRetflightno(retFlightnos);
                roundTripFlightInfo.setRetinfo(retFsl);
                flightList.add(roundTripFlightInfo);
            }
        } catch (Exception e) {
            logger.error("获取首页的往返信息异常", e);
            processResultInfo.setStatus(Constants.PARSING_FAIL);
            processResultInfo.setRet(false);
            return processResultInfo;
        }
        processResultInfo.setRet(true);
        processResultInfo.setStatus(Constants.SUCCESS);
        processResultInfo.setData(flightList);
        return processResultInfo;
    }

    @Override
    public BookingResult getBookingInfo(FlightSearchParam param) {
        BookingResult bookingResult = new BookingResult();
        BookingInfo bookingInfo = new BookingInfo();
        bookingInfo.setAction("http://www.scal.com.cn/B2C/ETicket/InterAirlineCenter");
        bookingInfo.setMethod("post");
        Map<String, String> inputs = new HashMap<String, String>();
        String depzh = param.getDep();
        String arrzh = param.getArr();
        String str = "{'AirlineType':'Round','IsFixedCabin':false,'RouteList':[{'RouteIndex':1,'RouteName':'去    程','OrgCity':'"+param.getDep()+"','DesCity':'"+param.getArr()+"','OrgCityName':'"+depzh+"','DesCityName':'"+arrzh+"','FlightDate':'"+param.getDepDate()+"'},{'RouteIndex':2,'RouteName':'返    程','OrgCity':'"+param.getArr()+"','DesCity':'"+param.getDep()+"','OrgCityName':'"+arrzh+"','DesCityName':'"+depzh+"','FlightDate':'"+param.getRetDate()+"'}],'AVType':0}";
        inputs.put("InterAirlineParamCenterJSON", str);
        bookingInfo.setInputs(inputs);
        bookingResult.setData(bookingInfo);
        bookingResult.setRet(true);
        return bookingResult;
    }

    public String getValue(String source, String regEx) {
        Matcher mm = Pattern.compile(regEx).matcher(source);
        return mm.find() ? mm.group(mm.groupCount() > 0 ? 1 : 0) : "";
    }
}
