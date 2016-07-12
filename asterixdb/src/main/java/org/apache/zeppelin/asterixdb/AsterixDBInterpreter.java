/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zeppelin.asterixdb;


import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterPropertyBuilder;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * Apache AsterixDB Interpreter for Zeppelin.
 */
public class AsterixDBInterpreter extends Interpreter {

  private static final String connectionAQL = "let $x := 'Hello World'; return $x";
  private static final String ASTERIXDB_HOST = "asterixdb.host";
  private static final String ASTERIXDB_PORT = "asterixdb.port";
  private static final String JSON_VIEWER = "<html><head>" +
      "<script src=\"http://rawgit.com/abodelot/jquery.json-viewer/master/json-viewer"
      + "/jquery.json-viewer.js\"></script>\n"
      + "<link href=\"http://rawgit.com/abodelot/jquery.json-viewer/master/json-viewer"
      + "/jquery.json-viewer.css\" "
      + "type=\"text/css\" rel=\"stylesheet\" />\n";
  private static Logger logger = LoggerFactory.getLogger(AsterixDBInterpreter.class);

  static {
    Interpreter.register("asterixdb", "asterixdb", AsterixDBInterpreter.class.getName(),
        new InterpreterPropertyBuilder()
            .add(ASTERIXDB_HOST, "localhost", "The host for AsterixDB HTTP API")
            .add(ASTERIXDB_PORT, "19002", "The port for AsterixDB HTTP API").build());
  }

  private final AsterixAPI api;

  public AsterixDBInterpreter(Properties property) {
    super(property);
    final String host = getProperty(ASTERIXDB_HOST);
    final String port = getProperty(ASTERIXDB_PORT);

    api = new AsterixAPI(host, port);

  }

  @Override public void open() {
    try {
      String helloWorld = api.executeAQL(connectionAQL);
      if (!helloWorld.contains("Hello World"))
        throw new IOException("AsterixDB did not return correct result: \n" + helloWorld);
    } catch (IOException e) {
      logger.error("Couldn't connect to AsterixDB HTTP API", e);
    }

  }

  @Override public void close() {
    logger.debug("Connection closed.");
  }

  @Override public InterpreterResult interpret(String aql, InterpreterContext context) {
    boolean flatten = "+flat".equals(aql.substring(0, 5));

    if (flatten)
      aql = aql.substring(5);
    String result = api.executeAQL(aql);
    if (result != null) {
      if (result.contains("error-code")) {
        JsonParser parser = new JsonParser();
        JsonArray error = parser.parse(result).getAsJsonObject().getAsJsonArray("error-code");
        return new InterpreterResult(InterpreterResult.Code.ERROR, error.get(1).getAsString());
      }
    }


    return new InterpreterResult(InterpreterResult.Code.SUCCESS,
        InterpreterResult.Type.HTML, format(result, context.getParagraphId()));
  }

  @Override public void cancel(InterpreterContext context) {

  }

  @Override public FormType getFormType() {
    return FormType.SIMPLE;
  }

  @Override public int getProgress(InterpreterContext context) {
    return 0;
  }

  private String format(String result, String id) {
    return JSON_VIEWER + "<script>$('#json-renderer" + id + "').jsonViewer(" + result
        + ");</script></head><body><pre id=\"json-renderer" + id + "\"></pre></body>";
  }


}
