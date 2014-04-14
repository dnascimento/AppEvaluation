/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.master;

import inesc.shared.AppEvaluationProtos.AppRequest;
import inesc.shared.AppEvaluationProtos.AppRequest.ReqType;
import inesc.shared.AppEvaluationProtos.Parameter;




/**
 * Generate request to Invoke the website (Highest level API)
 * The output is a protocol buffer to avoid cast to and from.
 * The class could have a builder but I used static to simplify.
 * 
 * @author darionascimento
 */
public class AskInterface {

    public static String baseURL = "http://localhost:8080";
    public static String JSON = "application/json";

    public static AppRequest getGoogle(int nExec) {
        return AppRequest.newBuilder().setType(ReqType.GET).setNExec(nExec).setUrl("http://google.at").build();
    }

    /**
     * Get homepage
     * 
     * @param nExec
     * @return
     */
    public static AppRequest getHomepage(int nExec) {
        return AppRequest.newBuilder().setType(ReqType.GET).setNExec(nExec).setUrl(baseURL + "/").build();
    }

    /**
     * Get New Question
     * 
     * @param nExec
     * @return
     */
    public static AppRequest getNewQuestion(int nExec) {
        return AppRequest.newBuilder().setType(ReqType.GET).setNExec(nExec).setUrl(baseURL + "/new-question").build();
    }


    /**
     * Post new question
     * 
     * @param nExec
     * @param title
     * @param tags
     * @param text
     * @return
     */
    public static AppRequest postNewQuestion(int nExec, String title, String tags, String text) {

        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.POST)
                                               .setNExec(nExec)
                                               .setUrl(baseURL + "/new-question");

        builder.addParameters(Parameter.newBuilder().setKey("title").setValue(title));
        builder.addParameters(Parameter.newBuilder().setKey("text").setValue(text));
        builder.addParameters(Parameter.newBuilder().setKey("tags").setValue(tags));

        return builder.build();
    }

    public static AppRequest deleteQuestion(int nExec, String questionTitle) {
        return AppRequest.newBuilder()
                         .setType(ReqType.DELETE)
                         .setNExec(nExec)
                         .setUrl(baseURL + "/question/" + questionTitle)
                         .build();
    }


    public static AppRequest getQuestion(int nExec, String questionTitle) {
        return AppRequest.newBuilder()
                         .setType(ReqType.DELETE)
                         .setNExec(nExec)
                         .setUrl(baseURL + "/question/" + questionTitle)
                         .build();
    }


    /**
     * Post new Answer
     * 
     * @param nExec
     * @param questionTitle
     * @param text
     * @return
     */
    public static AppRequest postAnswer(int nExec, String questionTitle, String text) {

        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.POST)
                                               .setNExec(nExec)
                                               .setContentType(JSON)
                                               .setUrl(baseURL + "/question/" + questionTitle + "/answer");

        builder.addParameters(Parameter.newBuilder().setKey("text").setValue(text));
        return builder.build();
    }






    public static AppRequest updateAnswer(int nExec, String questionTitle, String answerID, String text) {

        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.PUT)
                                               .setNExec(nExec)
                                               .setContentType(JSON)
                                               .setUrl(baseURL + "/question/" + questionTitle + "/answer");
        builder.addParameters(Parameter.newBuilder().setKey("answerID").setValue(answerID));
        builder.addParameters(Parameter.newBuilder().setKey("text").setValue(text));
        return builder.build();
    }



    public static AppRequest deleteAnswer(int nExec, String questionTitle, String answerID, String text) {

        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.DELETE)
                                               .setNExec(nExec)
                                               .setContentType(JSON)
                                               .setUrl(baseURL + "/question/" + questionTitle + "/answer");
        builder.addParameters(Parameter.newBuilder().setKey("answerID").setValue(answerID));
        builder.addParameters(Parameter.newBuilder().setKey("text").setValue(text));
        return builder.build();
    }


    // ///////// comments ////////////////////

    public static AppRequest postComment(int nExec, String questionTitle, String answerID, String text) {

        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.POST)
                                               .setNExec(nExec)
                                               .setContentType(JSON)
                                               .setUrl(baseURL + "/question/" + questionTitle + "/comment");
        builder.addParameters(Parameter.newBuilder().setKey("answerID").setValue(answerID));
        builder.addParameters(Parameter.newBuilder().setKey("text").setValue(text));
        return builder.build();
    }




    public static AppRequest updateComment(
            int nExec,
                String questionTitle,
                String answerID,
                String commentID,
                String text) {

        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.PUT)
                                               .setNExec(nExec)
                                               .setContentType(JSON)
                                               .setUrl(baseURL + "/question/" + questionTitle + "/comment");
        builder.addParameters(Parameter.newBuilder().setKey("answerID").setValue(answerID));
        builder.addParameters(Parameter.newBuilder().setKey("commentID").setValue(commentID));
        builder.addParameters(Parameter.newBuilder().setKey("text").setValue(text));
        return builder.build();
    }

    public static AppRequest deleteAnswer(
            int nExec,
                String questionTitle,
                String answerID,
                String commentID,
                String text) {

        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.DELETE)
                                               .setNExec(nExec)
                                               .setContentType(JSON)
                                               .setUrl(baseURL + "/question/" + questionTitle + "/comment");
        builder.addParameters(Parameter.newBuilder().setKey("answerID").setValue(answerID));
        builder.addParameters(Parameter.newBuilder().setKey("commentID").setValue(commentID));
        builder.addParameters(Parameter.newBuilder().setKey("text").setValue(text));
        return builder.build();
    }

    // ////////// VOTE /////////////////

    public static AppRequest voteUp(int nExec, String questionTitle, String answerID) {

        return vote(nExec, questionTitle, answerID, "up");
    }

    public static AppRequest voteDown(int nExec, String questionTitle, String answerID) {

        return vote(nExec, questionTitle, answerID, "down");
    }

    public static AppRequest vote(int nExec, String questionTitle, String answerID, String orientation) {

        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.POST)
                                               .setNExec(nExec)
                                               .setContentType(JSON)
                                               .setUrl(baseURL + "/question/" + questionTitle + "/" + orientation);

        builder.addParameters(Parameter.newBuilder().setKey("answerID").setValue(answerID));
        return builder.build();
    }




}
