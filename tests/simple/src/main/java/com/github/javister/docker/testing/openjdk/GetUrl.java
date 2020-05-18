package com.github.javister.docker.testing.openjdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import static java.lang.System.exit;
import static java.lang.System.out;

public class GetUrl {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            out.println("Usage:");
            out.println("    java -cp simple.jar com.github.javister.docker.testing.openjdk.GetUrl <url>");
            exit(-1);
        }

        URL url = new URL(args[0]);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        try {
            if (connection.getResponseCode() != 200) {
                out.println("Response code: " + connection.getResponseCode());
                exit(connection.getResponseCode());
            }
            try (InputStream is = url.openStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                out.println(sb.toString());
            }
        } finally {
            connection.disconnect();
        }
    }
}
