package com.quantTrading.backend.ibkr;


import com.ib.client.*;
import java.util.*;

public class IbkrBase implements EWrapper {

    @Override
    public void tickPrice(int i, int i1, double v, TickAttrib tickAttrib) {
        throw new RuntimeException("Unexpected call to tickPrice");
    }

    @Override
    public void tickSize(int i, int i1, Decimal decimal) {
        throw new RuntimeException("Unexpected call to tickSize");
    }

    @Override
    public void tickOptionComputation(int i, int i1, int i2, double v, double v1, double v2, double v3, double v4, double v5, double v6, double v7) {
        throw new RuntimeException("Unexpected call to tickOptionComputation");
    }

    @Override
    public void tickGeneric(int i, int i1, double v) {
        throw new RuntimeException("Unexpected call to tickGeneric");
    }

    @Override
    public void tickString(int i, int i1, String s) {
        throw new RuntimeException("Unexpected call to tickString");
    }

    @Override
    public void tickEFP(int i, int i1, double v, String s, double v1, int i2, String s1, double v2, double v3) {
        throw new RuntimeException("Unexpected call to tickEFP");
    }

    @Override
    public void orderStatus(int i, String s, Decimal decimal, Decimal decimal1, double v, int i1, int i2, double v1, int i3, String s1, double v2) {
        throw new RuntimeException("Unexpected call to orderStatus");
    }

    @Override
    public void openOrder(int i, Contract contract, Order order, OrderState orderState) {
        throw new RuntimeException("Unexpected call to openOrder");
    }

    @Override
    public void openOrderEnd() {
        throw new RuntimeException("Unexpected call to openOrderEnd");
    }

    @Override
    public void updateAccountValue(String s, String s1, String s2, String s3) {
        throw new RuntimeException("Unexpected call to updateAccountValue");
    }

    @Override
    public void updatePortfolio(Contract contract, Decimal decimal, double v, double v1, double v2, double v3, double v4, String s) {
        throw new RuntimeException("Unexpected call to updatePortfolio");
    }

    @Override
    public void updateAccountTime(String s) {
        throw new RuntimeException("Unexpected call to updateAccountTime");
    }

    @Override
    public void accountDownloadEnd(String s) {
        throw new RuntimeException("Unexpected call to accountDownloadEnd");
    }

    @Override
    public void nextValidId(int i) {
        throw new RuntimeException("Unexpected call to nextValidId");
    }

    @Override
    public void contractDetails(int i, ContractDetails contractDetails) {
        throw new RuntimeException("Unexpected call to contractDetails");
    }

    @Override
    public void bondContractDetails(int i, ContractDetails contractDetails) {
        throw new RuntimeException("Unexpected call to bondContractDetails");
    }

    @Override
    public void contractDetailsEnd(int i) {
        throw new RuntimeException("Unexpected call to contractDetailsEnd");
    }

    @Override
    public void execDetails(int i, Contract contract, Execution execution) {
        throw new RuntimeException("Unexpected call to execDetails");
    }

    @Override
    public void execDetailsEnd(int i) {
        throw new RuntimeException("Unexpected call to execDetailsEnd");
    }

    @Override
    public void updateMktDepth(int i, int i1, int i2, int i3, double v, Decimal decimal) {
        throw new RuntimeException("Unexpected call to updateMktDepth");
    }

    @Override
    public void updateMktDepthL2(int i, int i1, String s, int i2, int i3, double v, Decimal decimal, boolean b) {
        throw new RuntimeException("Unexpected call to updateMktDepthL2");
    }

    @Override
    public void updateNewsBulletin(int i, int i1, String s, String s1) {
        throw new RuntimeException("Unexpected call to updateNewsBulletin");
    }

    @Override
    public void managedAccounts(String s) {
        throw new RuntimeException("Unexpected call to managedAccounts");
    }

    @Override
    public void receiveFA(int i, String s) {
        throw new RuntimeException("Unexpected call to receiveFA");
    }

    @Override
    public void historicalData(int i, Bar bar) {
        throw new RuntimeException("Unexpected call to historicalData");
    }

    @Override
    public void scannerParameters(String s) {
        throw new RuntimeException("Unexpected call to scannerParameters");
    }

    @Override
    public void scannerData(int i, int i1, ContractDetails contractDetails, String s, String s1, String s2, String s3) {
        throw new RuntimeException("Unexpected call to scannerData");
    }

    @Override
    public void scannerDataEnd(int i) {
        throw new RuntimeException("Unexpected call to scannerDataEnd");
    }

    @Override
    public void realtimeBar(int i, long l, double v, double v1, double v2, double v3, Decimal decimal, Decimal decimal1, int i1) {
        throw new RuntimeException("Unexpected call to realtimeBar");
    }

    @Override
    public void currentTime(long l) {
        throw new RuntimeException("Unexpected call to currentTime");
    }

    @Override
    public void fundamentalData(int i, String s) {
        throw new RuntimeException("Unexpected call to fundamentalData");
    }

    @Override
    public void deltaNeutralValidation(int i, DeltaNeutralContract deltaNeutralContract) {
        throw new RuntimeException("Unexpected call to deltaNeutralValidation");
    }

    @Override
    public void tickSnapshotEnd(int i) {
        throw new RuntimeException("Unexpected call to tickSnapshotEnd");
    }

    @Override
    public void marketDataType(int i, int i1) {
        throw new RuntimeException("Unexpected call to marketDataType");
    }

    @Override
    public void commissionReport(CommissionReport commissionReport) {
        throw new RuntimeException("Unexpected call to commissionReport");
    }

    @Override
    public void position(String s, Contract contract, Decimal decimal, double v) {
        throw new RuntimeException("Unexpected call to position");
    }

    @Override
    public void positionEnd() {
        throw new RuntimeException("Unexpected call to positionEnd");
    }

    @Override
    public void accountSummary(int i, String s, String s1, String s2, String s3) {
        throw new RuntimeException("Unexpected call to accountSummary");
    }

    @Override
    public void accountSummaryEnd(int i) {
        throw new RuntimeException("Unexpected call to accountSummaryEnd");
    }

    @Override
    public void verifyMessageAPI(String s) {
        throw new RuntimeException("Unexpected call to verifyMessageAPI");
    }

    @Override
    public void verifyCompleted(boolean b, String s) {
        throw new RuntimeException("Unexpected call to verifyCompleted");
    }

    @Override
    public void verifyAndAuthMessageAPI(String s, String s1) {
        throw new RuntimeException("Unexpected call to verifyAndAuthMessageAPI");
    }

    @Override
    public void verifyAndAuthCompleted(boolean b, String s) {
        throw new RuntimeException("Unexpected call to verifyAndAuthCompleted");
    }

    @Override
    public void displayGroupList(int i, String s) {
        throw new RuntimeException("Unexpected call to displayGroupList");
    }

    @Override
    public void displayGroupUpdated(int i, String s) {
        throw new RuntimeException("Unexpected call to displayGroupUpdated");
    }

    @Override
    public void error(Exception e) {
        throw new RuntimeException("Unexpected call to error");
    }

    @Override
    public void error(String s) {
        throw new RuntimeException("Unexpected call to error");
    }

    @Override
    public void error(int i, int i1, String s, String s1) {
        throw new RuntimeException("Unexpected call to error");
    }

    @Override
    public void connectionClosed() {
        throw new RuntimeException("Unexpected call to connectionClosed");
    }

    @Override
    public void connectAck() {
        throw new RuntimeException("Unexpected call to connectAck");
    }

    @Override
    public void positionMulti(int i, String s, String s1, Contract contract, Decimal decimal, double v) {
        throw new RuntimeException("Unexpected call to positionMulti");
    }

    @Override
    public void positionMultiEnd(int i) {
        throw new RuntimeException("Unexpected call to positionMultiEnd");
    }

    @Override
    public void accountUpdateMulti(int i, String s, String s1, String s2, String s3, String s4) {
        throw new RuntimeException("Unexpected call to accountUpdateMulti");
    }

    @Override
    public void accountUpdateMultiEnd(int i) {
        throw new RuntimeException("Unexpected call to accountUpdateMultiEnd");
    }

    @Override
    public void securityDefinitionOptionalParameter(int i, String s, int i1, String s1, String s2, Set<String> set, Set<Double> set1) {
        throw new RuntimeException("Unexpected call to securityDefinitionOptionalParameter");
    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int i) {
        throw new RuntimeException("Unexpected call to securityDefinitionOptionalParameterEnd");
    }

    @Override
    public void softDollarTiers(int i, SoftDollarTier[] softDollarTiers) {
        throw new RuntimeException("Unexpected call to softDollarTiers");
    }

    @Override
    public void familyCodes(FamilyCode[] familyCodes) {
        throw new RuntimeException("Unexpected call to familyCodes");
    }

    @Override
    public void symbolSamples(int i, ContractDescription[] contractDescriptions) {
        throw new RuntimeException("Unexpected call to symbolSamples");
    }

    @Override
    public void historicalDataEnd(int i, String s, String s1) {
        throw new RuntimeException("Unexpected call to historicalDataEnd");
    }

    @Override
    public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {
        throw new RuntimeException("Unexpected call to mktDepthExchanges");
    }

    @Override
    public void tickNews(int i, long l, String s, String s1, String s2, String s3) {
        throw new RuntimeException("Unexpected call to tickNews");
    }

    @Override
    public void smartComponents(int i, Map<Integer, Map.Entry<String, Character>> map) {
        throw new RuntimeException("Unexpected call to smartComponents");
    }

    @Override
    public void tickReqParams(int i, double v, String s, int i1) {
        throw new RuntimeException("Unexpected call to tickReqParams");
    }

    @Override
    public void newsProviders(NewsProvider[] newsProviders) {
        throw new RuntimeException("Unexpected call to newsProviders");
    }

    @Override
    public void newsArticle(int i, int i1, String s) {
        throw new RuntimeException("Unexpected call to newsArticle");
    }

    @Override
    public void historicalNews(int i, String s, String s1, String s2, String s3) {
        throw new RuntimeException("Unexpected call to historicalNews");
    }

    @Override
    public void historicalNewsEnd(int i, boolean b) {
        throw new RuntimeException("Unexpected call to historicalNewsEnd");
    }

    @Override
    public void headTimestamp(int i, String s) {
        throw new RuntimeException("Unexpected call to headTimestamp");
    }

    @Override
    public void histogramData(int i, List<HistogramEntry> list) {
        throw new RuntimeException("Unexpected call to histogramData");
    }

    @Override
    public void historicalDataUpdate(int i, Bar bar) {
        throw new RuntimeException("Unexpected call to historicalDataUpdate");
    }

    @Override
    public void rerouteMktDataReq(int i, int i1, String s) {
        throw new RuntimeException("Unexpected call to rerouteMktDataReq");
    }

    @Override
    public void rerouteMktDepthReq(int i, int i1, String s) {
        throw new RuntimeException("Unexpected call to rerouteMktDepthReq");
    }

    @Override
    public void marketRule(int i, PriceIncrement[] priceIncrements) {
        throw new RuntimeException("Unexpected call to marketRule");
    }

    @Override
    public void pnl(int i, double v, double v1, double v2) {
        throw new RuntimeException("Unexpected call to pnl");
    }

    @Override
    public void pnlSingle(int i, Decimal decimal, double v, double v1, double v2, double v3) {
        throw new RuntimeException("Unexpected call to pnlSingle");
    }

    @Override
    public void historicalTicks(int i, List<HistoricalTick> list, boolean b) {
        throw new RuntimeException("Unexpected call to historicalTicks");
    }

    @Override
    public void historicalTicksBidAsk(int i, List<HistoricalTickBidAsk> list, boolean b) {
        throw new RuntimeException("Unexpected call to historicalTicksBidAsk");
    }

    @Override
    public void historicalTicksLast(int i, List<HistoricalTickLast> list, boolean b) {
        throw new RuntimeException("Unexpected call to historicalTicksLast");
    }

    @Override
    public void tickByTickAllLast(int i, int i1, long l, double v, Decimal decimal, TickAttribLast tickAttribLast, String s, String s1) {
        throw new RuntimeException("Unexpected call to tickByTickAllLast");
    }

    @Override
    public void tickByTickBidAsk(int i, long l, double v, double v1, Decimal decimal, Decimal decimal1, TickAttribBidAsk tickAttribBidAsk) {
        throw new RuntimeException("Unexpected call to tickByTickBidAsk");
    }

    @Override
    public void tickByTickMidPoint(int i, long l, double v) {
        throw new RuntimeException("Unexpected call to tickByTickMidPoint");
    }

    @Override
    public void orderBound(long l, int i, int i1) {
        throw new RuntimeException("Unexpected call to orderBound");
    }

    @Override
    public void completedOrder(Contract contract, Order order, OrderState orderState) {
        throw new RuntimeException("Unexpected call to completedOrder");
    }

    @Override
    public void completedOrdersEnd() {
        throw new RuntimeException("Unexpected call to completedOrdersEnd");
    }

    @Override
    public void replaceFAEnd(int i, String s) {
        throw new RuntimeException("Unexpected call to replaceFAEnd");
    }

    @Override
    public void wshMetaData(int i, String s) {
        throw new RuntimeException("Unexpected call to wshMetaData");
    }

    @Override
    public void wshEventData(int i, String s) {
        throw new RuntimeException("Unexpected call to wshEventData");
    }

    @Override
    public void historicalSchedule(int i, String s, String s1, String s2, List<HistoricalSession> list) {
        throw new RuntimeException("Unexpected call to historicalSchedule");
    }

    @Override
    public void userInfo(int i, String s) {
        throw new RuntimeException("Unexpected call to userInfo");
    }
}
