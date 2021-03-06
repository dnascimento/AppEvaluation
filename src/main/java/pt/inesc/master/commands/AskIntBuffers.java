/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package pt.inesc.master.commands;

import inesc.shared.AppEvaluationProtos.HistoryMsg.AppRequest;
import inesc.shared.AppEvaluationProtos.HistoryMsg.AppRequest.Builder;
import inesc.shared.AppEvaluationProtos.HistoryMsg.AppRequest.Parameter;
import inesc.shared.AppEvaluationProtos.HistoryMsg.AppRequest.ReqType;
import pt.inesc.share.AskInterface;

/**
 * Generate request to Invoke the website (Highest level API)
 * The output is a protocol buffer to avoid cast to and from.
 * The class could have a builder but I used to simplify.
 * 
 * @author darionascimento
 */
public class AskIntBuffers extends
        AskInterface<AppRequest.Builder> {

    public String JSON = "application/json";

    @Override
    public AppRequest.Builder getGoogle() {
        return AppRequest.newBuilder().setType(ReqType.GET).setUrl("http://google.at");
    }

    /**
     * Get homepage
     * 
     * @param nExec
     * @return
     */
    @Override
    public AppRequest.Builder getHomepage(String serverURL) {
        return AppRequest.newBuilder().setType(ReqType.GET).setUrl(serverURL + "/");
    }

    /**
     * Get New Question
     * 
     * @param nExec
     * @return
     */
    @Override
    public AppRequest.Builder getNewQuestion(String serverURL) {
        return AppRequest.newBuilder().setType(ReqType.GET).setUrl(serverURL + "/new-question");
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
    @Override
    public AppRequest.Builder postNewQuestion(
            String serverURL,
                String title,
                String tags,
                String text,
                String author,
                String views,
                String answers,
                String answerId) {
        AppRequest.Builder builder = AppRequest.newBuilder().setType(ReqType.POST).setUrl(serverURL + "/new-question");

        builder.addParameters(Parameter.newBuilder().setKey("title").setValue(title));
        builder.addParameters(Parameter.newBuilder().setKey("text").setValue(text));
        builder.addParameters(Parameter.newBuilder().setKey("tags").setValue(tags));
        builder.addParameters(Parameter.newBuilder().setKey("author").setValue(author));
        builder.addParameters(Parameter.newBuilder().setKey("views").setValue(views));
        builder.addParameters(Parameter.newBuilder().setKey("answers").setValue(answers));

        return builder;
    }

    @Override
    public AppRequest.Builder deleteQuestion(String serverURL, String questionTitle) {
        return AppRequest.newBuilder().setType(ReqType.DELETE).setUrl(serverURL + "/question/" + questionTitle);
    }


    @Override
    public AppRequest.Builder getQuestion(String serverURL, String questionTitle) {
        return AppRequest.newBuilder().setType(ReqType.DELETE).setUrl(serverURL + "/question/" + questionTitle);
    }


    /**
     * Post new Answer
     * 
     * @param nExec
     * @param questionTitle
     * @param text
     * @return
     */
    @Override
    public AppRequest.Builder postAnswer(String serverURL, String questionTitle, String text, String author, String answerId) {
        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.POST)
                                               .setContentType(JSON)
                                               .setUrl(serverURL + "/question/" + questionTitle + "/answer");

        builder.addParameters(Parameter.newBuilder().setKey("text").setValue(text));
        builder.addParameters(Parameter.newBuilder().setKey("author").setValue(author));
        return builder;
    }






    @Override
    public AppRequest.Builder updateAnswer(String serverURL, String questionTitle, String answerID, String text) {

        AppRequest.Builder builder = AppRequest.newBuilder().setType(ReqType.PUT)

        .setContentType(JSON).setUrl(serverURL + "/question/" + questionTitle + "/answer");
        builder.addParameters(Parameter.newBuilder().setKey("answerID").setValue(answerID));
        builder.addParameters(Parameter.newBuilder().setKey("text").setValue(text));
        return builder;
    }



    @Override
    public AppRequest.Builder deleteAnswer(String serverURL, String questionTitle, String answerID) {

        AppRequest.Builder builder = AppRequest.newBuilder().setType(ReqType.DELETE)

        .setContentType(JSON).setUrl(serverURL + "/question/" + questionTitle + "/answer");
        builder.addParameters(Parameter.newBuilder().setKey("answerID").setValue(answerID));
        return builder;
    }


    // ///////// comments ////////////////////

    @Override
    public AppRequest.Builder postComment(String serverURL, String questionTitle, String answerID, String text, String author) {

        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.POST)
                                               .setContentType(JSON)
                                               .setUrl(serverURL + "/question/" + questionTitle + "/comment");
        builder.addParameters(Parameter.newBuilder().setKey("answerID").setValue(answerID));
        builder.addParameters(Parameter.newBuilder().setKey("text").setValue(text));
        builder.addParameters(Parameter.newBuilder().setKey("author").setValue(author));

        return builder;
    }




    @Override
    public AppRequest.Builder updateComment(String serverURL, String questionTitle, String answerID, String commentID, String text) {

        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.PUT)
                                               .setContentType(JSON)
                                               .setUrl(serverURL + "/question/" + questionTitle + "/comment");
        builder.addParameters(Parameter.newBuilder().setKey("answerID").setValue(answerID));
        builder.addParameters(Parameter.newBuilder().setKey("commentID").setValue(commentID));
        builder.addParameters(Parameter.newBuilder().setKey("text").setValue(text));
        return builder;
    }

    @Override
    public AppRequest.Builder deleteComment(String serverURL, String questionTitle, String answerID, String commentID) {
        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.DELETE)
                                               .setContentType(JSON)
                                               .setUrl(serverURL + "/question/" + questionTitle + "/comment");
        builder.addParameters(Parameter.newBuilder().setKey("answerID").setValue(answerID));
        builder.addParameters(Parameter.newBuilder().setKey("commentID").setValue(commentID));
        return builder;
    }

    // ////////// VOTE /////////////////
    @Override
    public AppRequest.Builder voteUp(String serverURL, String questionTitle, String answerID) {

        return vote(serverURL, questionTitle, answerID, "up");
    }

    @Override
    public AppRequest.Builder voteDown(String serverURL, String questionTitle, String answerID) {

        return vote(serverURL, questionTitle, answerID, "down");
    }

    private AppRequest.Builder vote(String serverURL, String questionTitle, String answerID, String orientation) {
        AppRequest.Builder builder = AppRequest.newBuilder()
                                               .setType(ReqType.POST)
                                               .setContentType(JSON)
                                               .setUrl(serverURL + "/question/" + questionTitle + "/" + orientation);

        builder.addParameters(Parameter.newBuilder().setKey("answerID").setValue(answerID));
        return builder;
    }

    public Builder newGet(String uri) {
        return AppRequest.newBuilder().setType(ReqType.GET).setUrl(uri);
    }






}
