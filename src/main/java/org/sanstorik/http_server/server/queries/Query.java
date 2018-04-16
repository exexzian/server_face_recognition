package org.sanstorik.http_server.server.queries;

import com.google.common.io.Files;
import javafx.util.Pair;
import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FileUtils;
import org.sanstorik.http_server.HttpResponse;
import org.sanstorik.http_server.Token;
import org.sanstorik.http_server.database.ConcreteSqlConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public abstract class Query {
    public enum Type {
        GET, POST, PUT, DELETE, UNIQUE;

        public static Type of(String type) {
            switch (type) {
                case "GET":
                    return Type.GET;
                case "POST":
                    return Type.POST;
                case "PUT":
                    return Type.PUT;
                case "DELETE":
                    return Type.DELETE;
                case "UNIQUE":
                    return Type.UNIQUE;
                default:
                    throw new IllegalArgumentException("No such type.");
            }
        }
    }

    private Type type = Type.POST;
    private boolean doCheckAuth = true;
    private final HttpResponse response = HttpResponse.fromTemplate();


    Query() { }


    Query(Type type) {
        this.type = type;
    }


    Query(boolean doCheckAuth) {
        this.doCheckAuth = doCheckAuth;
    }


    Query(Type type, boolean doCheckAuth) {
        this.type = type;
        this.doCheckAuth = doCheckAuth;
    }


    /**
     * Parse url and data from request and create appropriate class
     * to handle it.
     *
     * @param request from user
     * @return query with needed response
     */
    public static Query fromRequest(HttpServletRequest request, ConcreteSqlConnection databaseConnection) {
        String url = request.getRequestURL().toString();
        String method = url.substring(url.lastIndexOf("/"), url.length());

        Query query;

        switch (method) {
            case "/register":
                query = new RegisterQuery();
                break;
            case "/login":
                query = new LoginQuery();
                break;
            case "/login_photo":
                query = new LoginPhotoQuery();
                break;
            case "/highlight_faces":
                query = new HighlightFacesQuery();
                break;
            case "/faces_coordinates":
                query = new FacesCoordinatesQuery();
                break;
            case "/identify_group":
                query = new IdentifyGroupQuery();
                break;
            case "/update_user_photo":
                query = new UpdateUserPhotoQuery();
                break;
            default:
                query = new NotSupportedQuery();
                break;
        }

        if (query.type != Type.of(request.getMethod()) &&
                query.type != Type.UNIQUE) {
            query = new NotSupportedQueryType();
        }

        System.out.println("Query = " + query.getClass().toString());

        Token token = null;

        //verify token and return if not verified
        if (query.doCheckAuth) {
            Pair<Token, String> tokenResponse = query.verifyToken(request);
            token = tokenResponse.getKey();

            if (tokenResponse.getKey() == null) {
                query.errorResponse(tokenResponse.getValue());
                return query;
            }
        }

        query.parseRequest(request, databaseConnection, token);

        return query;
    }


    public final String asJsonResponse() {
        return response.asJson();
    }


    /**
     * Main method that checks input and is making a response.
     * Override this to proccess specific query.
     * Use token if you have to use it.
     * @param request from user
     * @param databaseConnection connection to database
     * @param token token sent by user. Avaialble only if auth check is set true.
     */
    protected abstract void parseRequest(HttpServletRequest request, ConcreteSqlConnection databaseConnection, Token token);


    protected void errorResponse(String message) {
        response.setErrorMessage(message);
        response.setStatusError();
    }


    protected void addParam(String key, String value) {
        response.addParam(key, value);
    }


    protected void addCustomEntry(String key, Map<String, String> values) {
        response.addEmbeddedEntry(key, values);
    }


    /**
     * Reads image from multipart request and writes it to server.
     * @param request query
     * @param key of image in request. For example <image>
     * @param directory where to create this image
     * @return image written to server with its url
     */
    protected Pair<File, String> readImageFromMultipartRequest(HttpServletRequest request, String key, String directory) {
        File image = null;
        String url = System.getenv("IMAGE_HOSTING_URL")  + "/" + directory;

        try {
            image = new File(url);
            if (!image.exists()) {
                image.createNewFile();
            }

            Part part = request.getPart(key);
            InputStream imageStream = part.getInputStream();

            FileUtils.copyInputStreamToFile(imageStream, image);
        } catch (IOException | ServletException e) {
            e.printStackTrace();
        }

        return new Pair<>(image, url);
    }


    /**
     * Method that verifies token from user request.
     *
     * @return token if verified, and null + message or error otherwise
     */
    private Pair<Token, String> verifyToken(HttpServletRequest request) {
        //token is in form {Bearer <token>}
        String token = request.getHeader("Authorization");
        if (token == null || token.length() < 8) {
            return new Pair<>(null, "Token is empty or too short.");
        }

        String errorMessage = "OK";
        token = token.substring(7);

        Token decipheredToken = Token.decypherToken(token);

        if (decipheredToken == null) {
            errorMessage = "Token is not verified. Invalid user. Make sure you've put Bearer in front.";
        } else if (decipheredToken.isExpired()) {
            errorMessage = "Token usability time has been expired. Create a new one.";
        }

        return new Pair<>(decipheredToken, errorMessage);
    }
}