package inesc.slave;

import inesc.share.AskInterface;
import inesc.shared.AppEvaluationProtos;
import inesc.shared.AppEvaluationProtos.AppRequest;
import inesc.shared.AppEvaluationProtos.AppRequest.ReqType;
import inesc.shared.AppEvaluationProtos.Parameter;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

public class RequestCreation extends
        AskInterface<HttpRequestBase> {
    private static Logger log = Logger.getLogger(RequestCreation.class);

    private static final String JSON = "application/json";



    /**
     * Convert Protocol Buffer to HTTP Package, the object used by HTTP Client
     * 
     * @param reqBuffer
     * @return HTTPRequest package
     * @throws UnsupportedEncodingException
     */
    public static HttpRequestBase convertReqBufferToHTTPRequest(AppRequest reqBuffer) throws UnsupportedEncodingException {
        // Convert parameters to from Buffer to HTTP
        Parameters parameters = new Parameters(reqBuffer.getParametersList());
        String url = reqBuffer.getUrl();
        boolean json = false;
        if (reqBuffer.hasContentType() && reqBuffer.getContentType().equals(JSON)) {
            json = true;
        }
        return createPacket(url, reqBuffer.getType(), parameters, json);
    }

    private static HttpRequestBase createPacket(String url, ReqType type, Parameters parameters, boolean json) {

        HttpRequestBase request = null;
        try {
            if (parameters == null) {
                parameters = new Parameters(new LinkedList<AppEvaluationProtos.Parameter>());
            }
            switch (type) {
            case GET:
                request = new HttpGet(url);
                break;
            case DELETE:
                request = new HttpDelete(url);
                break;
            case PUT:
                HttpPut put = new HttpPut(url);
                if (json) {
                    put.setHeader("Content-Type", JSON);
                    put.setEntity(parametersToJson(parameters));
                } else {
                    put.setEntity(new UrlEncodedFormEntity(parameters.build()));
                }
                request = put;
                break;
            case POST:
                HttpPost post = new HttpPost(url);
                if (json) {
                    post.setHeader("Content-Type", JSON);
                    post.setEntity(parametersToJson(parameters));
                } else {
                    post.setEntity(new UrlEncodedFormEntity(parameters.build()));
                }
                request = post;
                break;
            }
        } catch (Exception e) {
            log.error(e);
        }
        return request;
    }









    private static StringEntity parametersToJson(Parameters parameters) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"");
        boolean first = true;
        for (NameValuePair p : parameters.build()) {
            if (first) {
                first = false;
            } else {
                sb.append(",\"");
            }
            sb.append(p.getName());
            sb.append("\":\"");
            sb.append(p.getValue());
            sb.append("\"");
        }
        sb.append("}");
        return new StringEntity(sb.toString());

    }

    /**
     * Convert Protobuff parameters to HTTP
     * 
     * @author darionascimento
     */
    private static class Parameters {
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

        public Parameters(String... paramString) {
            for (int i = 0; i < paramString.length;) {
                String name = paramString[i++];
                String value = paramString[i++];
                params.add(new BasicNameValuePair(name, value));
            }
        }


        public Parameters(List<Parameter> parametersList) {
            for (Parameter param : parametersList) {
                this.add(param.getKey(), param.getValue());
            }
        }

        public Parameters add(String name, String value) {
            params.add(new BasicNameValuePair(name, value));
            return this;
        }

        public ArrayList<NameValuePair> build() {
            return params;
        }
    }

    @Override
    public HttpRequestBase getGoogle() {
        return createPacket("http://google.pt", ReqType.GET, null, false);
    }

    @Override
    public HttpRequestBase getHomepage(String serverURL) {
        return createPacket(serverURL + "/", ReqType.GET, null, false);
    }

    @Override
    public HttpRequestBase getNewQuestion(String serverURL) {
        return createPacket(serverURL + "/new-question", ReqType.GET, null, false);
    }


    @Override
    public HttpRequestBase postNewQuestion(
            String serverURL,
                String title,
                String tags,
                String text,
                String author,
                String views,
                String answers,
                String answerId) {
        Parameters p = new Parameters("title", title, "text", text, "tags", tags, "author", author, "views", views, "answers", answers,
                "answerId", answerId);
        return createPacket(serverURL + "/new-question", ReqType.POST, p, false);
    }

    @Override
    public HttpRequestBase deleteQuestion(String serverURL, String questionTitle) {
        questionTitle = escapeText(questionTitle);
        return createPacket(serverURL + "/question/" + questionTitle, ReqType.DELETE, null, true);
    }


    @Override
    public HttpRequestBase getQuestion(String serverURL, String questionTitle) {
        questionTitle = escapeText(questionTitle);
        return createPacket(serverURL + "/question/" + questionTitle, ReqType.GET, null, false);
    }


    @Override
    public HttpRequestBase postAnswer(String serverURL, String questionTitle, String text, String author, String answerId) {
        Parameters p = new Parameters("text", text, "author", author, "answerId", answerId);
        questionTitle = escapeText(questionTitle);
        return createPacket(serverURL + "/question/" + questionTitle + "/answer", ReqType.POST, p, true);
    }

    @Override
    public HttpRequestBase updateAnswer(String serverURL, String questionTitle, String answerID, String text) {
        Parameters p = new Parameters("answerID", answerID, "text", text);
        questionTitle = escapeText(questionTitle);
        return createPacket(serverURL + "/question/" + questionTitle + "/answer", ReqType.PUT, p, true);
    }


    @Override
    public HttpRequestBase deleteAnswer(String serverURL, String questionTitle, String answerID, String text) {
        Parameters p = new Parameters("answerID", answerID, "text", text);
        questionTitle = escapeText(questionTitle);
        return createPacket(serverURL + "/question/" + questionTitle + "/answer", ReqType.DELETE, p, true);
    }



    @Override
    public HttpRequestBase postComment(String serverURL, String questionTitle, String answerID, String text, String author) {
        Parameters p = new Parameters("answerID", answerID, "text", text, "author", author);
        questionTitle = escapeText(questionTitle);
        return createPacket(serverURL + "/question/" + questionTitle + "/comment", ReqType.POST, p, true);
    }



    @Override
    public HttpRequestBase updateComment(String serverURL, String questionTitle, String answerID, String commentID, String text) {
        Parameters p = new Parameters("answerID", "commentID", commentID, answerID, "text", text);
        questionTitle = escapeText(questionTitle);
        return createPacket(serverURL + "/question/" + questionTitle + "/comment", ReqType.PUT, p, true);
    }



    @Override
    public HttpRequestBase deleteComment(String serverURL, String questionTitle, String answerID, String commentID) {
        Parameters p = new Parameters("answerID", answerID, "commentID", commentID);
        questionTitle = escapeText(questionTitle);
        return createPacket(serverURL + "/question/" + questionTitle + "/comment", ReqType.DELETE, p, true);
    }



    @Override
    public HttpRequestBase voteUp(String serverURL, String questionTitle, String answerID) {
        return vote(serverURL, questionTitle, answerID, "up");
    }


    @Override
    public HttpRequestBase voteDown(String serverURL, String questionTitle, String answerID) {
        return vote(serverURL, questionTitle, answerID, "down");
    }

    private HttpRequestBase vote(String serverURL, String questionTitle, String answerID, String orientation) {
        Parameters p = new Parameters("answerID", answerID);
        questionTitle = escapeText(questionTitle);
        return createPacket(serverURL + "/question/" + questionTitle + "/" + orientation, ReqType.POST, p, true);
    }


    private String escapeText(String text) {
        URLCodec codec = new URLCodec();
        try {
            return codec.encode(text);
        } catch (EncoderException e1) {
            log.error(e1);
            return text;
        }
    }

}
