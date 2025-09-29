
package com.freehands.assistant.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("health")
    Call<String> health();
}
