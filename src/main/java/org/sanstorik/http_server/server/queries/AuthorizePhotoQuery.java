package org.sanstorik.http_server.server.queries;

import org.sanstorik.http_server.Token;
import org.sanstorik.http_server.database.ConcreteSqlConnection;
import org.sanstorik.http_server.utils.FileUtils;
import org.sanstorik.neural_network.face_detection.Face;
import org.sanstorik.neural_network.face_identifying.FaceRecognizer;
import org.sanstorik.neural_network.face_identifying.FullFaceFeatures;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

public class AuthorizePhotoQuery extends FaceFeatureQuery {


    @Override protected void parseRequest(HttpServletRequest request, ConcreteSqlConnection databaseConnection, Token token) {
        Face.Response<File, String> response = readImageFromMultipartRequest(request, "image",
                FileUtils.getRootCachedImagesDirectoryName(), FileUtils.generateRandomImageName());

        if (response.left == null) {
            errorResponse("No image given.");
            return;
        }

        FaceRecognizer recognizer = FaceRecognizer.create();

        FullFaceFeatures expectedFeature = getFeatureOfUser(token.getUserId(), databaseConnection);
        if (expectedFeature == null) {
            errorResponse("Couldn't match users from database with a photo.");
            return;
        }

        FaceRecognizer.Prediction prediction = recognizer.identify(response.left, expectedFeature);

        if (prediction == null) {
            errorResponse("User photo couldn't be proceeded. Make sure a face on the photo is valid.");
            return;
        }

        addParam("max_probability", String.valueOf(prediction.getPercentage()));
        addParam("matched", String.valueOf(prediction.isIdentified()));

        String granted = prediction.isIdentified() ? "granted" : "not granted";
        addParam("message", "Access is " + granted + " for user " + token.getUsername());
    }
}
