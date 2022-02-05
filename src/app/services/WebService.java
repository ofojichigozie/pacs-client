package app.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface WebService {

	@FormUrlEncoded
	@POST("api/v1/login")
	Call<JsonObject> authenticate(
			@Field("accessKey") String accessKey
	);

	@FormUrlEncoded
	@POST("api/v1/patient")
	Call<JsonObject> addPatient(
			@Header("Authorization") String bearerToken,
			@Field("firstName") String firstName,
			@Field("lastName") String lastName,
			@Field("sex") String sex,
			@Field("dateOfBirth") String dateOfBirth,
			@Field("modalities") String modalities
	);

	@GET("api/v1/patient/{accessCode}")
	Call<JsonObject> getPatient(
			@Header("Authorization") String bearerToken,
			@Path("accessCode") String accessCode
	);

	@Multipart
	@POST("api/v1/patient/image")
	Call<JsonObject> savePatientImage(
			@Header("Authorization") String bearerToken,
			@Part("patientId") RequestBody patientId,
			@Part MultipartBody.Part image
	);

	@GET("api/v1/patient/images/{patientId}")
	Call<JsonObject> retrievePatientImages(
			@Header("Authorization") String bearerToken,
			@Path("patientId") String patientId
	);
}
