/*
 * Author: Dario Nascimento (dario.nascimento@tecnico.ulisboa.pt)
 * 
 * Instituto Superior Tecnico - University of Lisbon - INESC-ID Lisboa
 * Copyright (c) 2014 - All rights reserved
 */

package inesc.share;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;

import com.google.common.io.BaseEncoding;





/**
 * Generate request to Invoke the website (Highest level API)
 * The output is a protocol buffer to avoid cast to and from.
 * The class could have a builder but I used static to simplify.
 * 
 * @author darionascimento
 */
public abstract class AskInterface<T> {

    public abstract T getGoogle();

    /**
     * Get homepage
     * 
     * @param nExec
     * @return
     */
    public abstract T getHomepage(String serverURL);

    /**
     * Get New Question
     * 
     * @param nExec
     * @return
     */
    public abstract T getNewQuestion(String serverURL);

    /**
     * Post new question
     * 
     * @param nExec
     * @param title
     * @param tags
     * @param text
     * @return
     */
    public abstract T postNewQuestion(String serverURL, String title, String tags, String text, String author);

    public abstract T deleteQuestion(String serverURL, String questionTitle);

    public abstract T getQuestion(String serverURL, String questionTitle);

    /**
     * Post new Answer
     * 
     * @param nExec
     * @param questionTitle
     * @param text
     * @return
     */
    public abstract T postAnswer(String serverURL, String questionTitle, String text, String author);



    public abstract T updateAnswer(String serverURL, String questionTitle, String answerID, String text);

    public abstract T deleteAnswer(String serverURL, String questionTitle, String answerID, String text);

    // ///////// comments ////////////////////

    public abstract T postComment(String serverURL, String questionTitle, String answerID, String text, String author);

    public abstract T updateComment(String serverURL, String questionTitle, String answerID, String commentID, String text);

    public abstract T deleteComment(String serverURL, String questionTitle, String answerID, String commentID);

    // //////////
    // VOTE
    // ////////////////

    public abstract T voteUp(String serverURL, String questionTitle, String answerID);

    public abstract T voteDown(String serverURL, String questionTitle, String answerID);

    public String generateAnswerId(String title, String author, String text) {
        MessageDigest md;
        try {
            title = new URLCodec().encode(title);
            md = MessageDigest.getInstance("MD5");
            System.out.println("New answer: " + title + author + text);
            byte[] digest = md.digest((title + author + text).getBytes("UTF-16"));
            String hashcode = BaseEncoding.base64().encode(digest);
            return hashcode;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (EncoderException e) {
            e.printStackTrace();
        }
        return null;
    }
}
