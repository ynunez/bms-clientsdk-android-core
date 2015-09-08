/*
 *     Copyright 2015 IBM Corp.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.ibm.mobilefirstplatform.clientsdk.android.core.api;

import android.content.Context;

import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthenticationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.security.api.AuthorizationManager;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.Utils;
import com.ibm.mobilefirstplatform.clientsdk.android.security.internal.challengehandlers.ChallengeHandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.HashMap;

/**
 * The BMSClient is a singleton that serves as the entry point to MobileFirst.
 */
public class BMSClient {
    public final static String HTTP_SCHEME = "http";
    public final static String HTTPS_SCHEME = "https";

    private final static String QUERY_PARAM_SUBZONE = "subzone";

    private static BMSClient instance = null;
    private String backendRoute;
    private String backendGUID;
    private String rewriteDomain;

    private int defaultTimeout = 20000;
    private HashMap<String, ChallengeHandler> challengeHandlers = new HashMap<>();

    /**
     * Should be called to obtain the instance of BMSClient.
     * @return the instance of BMSClient.
     */
    public static BMSClient getInstance() {
        if (instance == null) {
            instance = new BMSClient();

            MFPRequest.setup(); //Set up network interceptor to log network event times analytics.
        }
        return instance;
    }

    private BMSClient() {
    }

    /**
     * Sets the base URL for the authorization server.
     * <p>
     * This method should be called before you send the first request that requires authorization.
     * </p>
     * @param context Android application context
     * @param backendRoute Specifies the base URL for the authorization server
     * @param backendGUID Specifies the GUID of the application
     * @throws MalformedURLException {@code backendRoute} could not be parsed as a URL.
     */
    public void initialize(Context context, String backendRoute, String backendGUID) throws MalformedURLException {
        this.backendGUID = backendGUID;
        this.backendRoute = backendRoute;
        this.rewriteDomain = null;
        String subzone = null;

        if (backendRoute != null) {
            URL url = new URL(backendRoute);

            String query = url.getQuery();
            if (query != null) {
                subzone = Utils.getParameterValueFromQuery(query, QUERY_PARAM_SUBZONE);
                this.backendRoute = backendRoute.substring(0, backendRoute.length() - query.length() - 1);
            }
        }

        this.rewriteDomain = Utils.buildRewriteDomain(this.backendRoute, subzone);
        AuthorizationManager.createInstance(context.getApplicationContext());
        Logger.setContext(context.getApplicationContext());
    }

    /**
     *
     * @return backend route url
     */
    public String getBackendRoute() {
        return backendRoute;
    }

    /**
     *
     * @return backend GUID
     */
    public String getBackendGUID() {
        return backendGUID;
    }

    /**
     * Registers authentication listener for specified realm.
     * @param realm authentication realm.
     * @param authenticationListener authentication listener.
     */
    public void registerAuthenticationListener(String realm, AuthenticationListener authenticationListener) {
        if (realm == null || realm.isEmpty()) {
            throw new InvalidParameterException("The realm name can't be null or empty.");
        }

        if (authenticationListener == null) {
            throw new InvalidParameterException("The authentication listener object can't be null.");
        }

        ChallengeHandler handler = new ChallengeHandler();
        handler.initialize(realm, authenticationListener);
        challengeHandlers.put(realm, handler);
    }

    /**
     * Unregisters authentication listener
     * @param realm the realm the listener was registered for
     */
    public void unregisterAuthenticationListener(String realm) {
        if (realm != null && !realm.isEmpty()) {
            challengeHandlers.remove(realm);
        }
    }

    /**
     * @exclude
     *
     * @param realm authentication realm
     * @return challenge handler for specified realm
     */
    public ChallengeHandler getChallengeHandler(String realm) {
        return challengeHandlers.get(realm);
    }

    /**
     *
     * @return default timeout, in milliseconds
     */
    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    /**
     * Sets the default timeout. The SDK's default value is 20000 milliseconds.
     * @param timeout specifies the new timeout, in milliseconds
     */
    public void setDefaultTimeout(int timeout) {
        defaultTimeout = timeout;
    }

    /**
     * @exclude
     * @return rewrite domain generated from backend route url.
     */
    public String getRewriteDomain() {
        return rewriteDomain;
    }
}
