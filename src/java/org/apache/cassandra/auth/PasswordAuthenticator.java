/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.auth;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.config.Schema;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.cql3.QueryProcessor;
import org.apache.cassandra.cql3.UntypedResultSet;
import org.apache.cassandra.cql3.statements.SelectStatement;
import org.apache.cassandra.exceptions.*;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.transport.messages.ResultMessage;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.mindrot.jbcrypt.BCrypt;

import static org.apache.cassandra.auth.CassandraRoleManager.consistencyForRole;

/**
 * PasswordAuthenticator is an IAuthenticator implementation
 * that keeps credentials (rolenames and bcrypt-hashed passwords)
 * internally in C* - in system_auth.roles CQL3 table.
 * Since 3.0, the management of roles (creation, modification,
 * querying etc is the responsibility of IRoleManager. Use of
 * PasswordAuthenticator requires the use of CassandraRoleManager
 * for storage & retrieval of encryted passwords.
 */
//对应system_auth.credentials表
//启动时，会先调用setup()方法
//    CREATE TABLE system_auth.credentials (
//        username text,
//        salted_hash text, //使用BCrypt算法
//        options map<text,text>, //这个字段目前未使用
//        PRIMARY KEY(username)
//    ) WITH gc_grace_seconds=90 * 24 * 60 * 60 // 3 months
public class PasswordAuthenticator implements IAuthenticator
{
    private static final Logger logger = LoggerFactory.getLogger(PasswordAuthenticator.class);

    // name of the hash column.
    private static final String SALTED_HASH = "salted_hash";

    // really this is a rolename now, but as it only matters for Thrift, we leave it for backwards compatibility
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";

//<<<<<<< HEAD
//    //SELECT salted_hash FROM system_auth.credentials WHERE username = ?
//    private SelectStatement authenticateStatement;  //在setup中初始化，使用prepare的方式
//=======
    private static final byte NUL = 0;
    private SelectStatement authenticateStatement;

    public static final String LEGACY_CREDENTIALS_TABLE = "credentials";
    private SelectStatement legacyAuthenticateStatement;

    // No anonymous access.
    public boolean requireAuthentication()
    {
        return true;
    }

    private AuthenticatedUser authenticate(String username, String password) throws AuthenticationException
    {
        try
        {
//<<<<<<< HEAD
//            //在system_auth.credentials表中按用户名取出对应密码的hash值
//            ResultMessage.Rows rows = authenticateStatement.execute(QueryState.forInternalCalls(),
//                                                                    QueryOptions.forInternalCalls(consistencyForUser(username),
//                                                                                                  Lists.newArrayList(ByteBufferUtil.bytes(username))));
//            result = UntypedResultSet.create(rows.result);
//        }
//        catch (RequestValidationException e)
//        {
//            throw new AssertionError(e); // not supposed to happen
//=======
            // If the legacy users table exists try to verify credentials there. This is to handle the case
            // where the cluster is being upgraded and so is running with mixed versions of the authn tables
            SelectStatement authenticationStatement = Schema.instance.getCFMetaData(AuthKeyspace.NAME, LEGACY_CREDENTIALS_TABLE) == null
                                                    ? authenticateStatement
                                                    : legacyAuthenticateStatement;
            return doAuthenticate(username, password, authenticationStatement);
        }
        catch (RequestExecutionException e)
        {
            logger.debug("Error performing internal authentication", e);
            throw new AuthenticationException(e.toString());
        }
//<<<<<<< HEAD
//
//        //password是明文，result.one().getString(SALTED_HASH)是hash值
//        if (result.isEmpty() || !BCrypt.checkpw(password, result.one().getString(SALTED_HASH)))
//            throw new AuthenticationException("Username and/or password are incorrect");
//
//        return new AuthenticatedUser(username);
//    }
//
//    //在system_auth.credentials表中新增一条记录只有这两个字段:(username, salted_hash)
//    //不包含options字段
//    public void create(String username, Map<Option, Object> options) throws InvalidRequestException, RequestExecutionException
//    {
//        String password = (String) options.get(Option.PASSWORD);
//        if (password == null)
//            throw new InvalidRequestException("PasswordAuthenticator requires PASSWORD option");
//
//        process(String.format("INSERT INTO %s.%s (username, salted_hash) VALUES ('%s', '%s')",
//                              Auth.AUTH_KS,
//                              CREDENTIALS_CF,
//                              escape(username),
//                              escape(hashpw(password))),
//                consistencyForUser(username));
//    }
//
//    //按用户名修改system_auth.credentials表中的salted_hash字段值
//    public void alter(String username, Map<Option, Object> options) throws RequestExecutionException
//    {
//        //options map<text,text>, //这个字段目前未使用
//        process(String.format("UPDATE %s.%s SET salted_hash = '%s' WHERE username = '%s'",
//                              Auth.AUTH_KS,
//                              CREDENTIALS_CF,
//                              escape(hashpw((String) options.get(Option.PASSWORD))),
//                              escape(username)),
//                consistencyForUser(username));
//    }
//
//    //删除username对应的记录
//    public void drop(String username) throws RequestExecutionException
//    {
//        process(String.format("DELETE FROM %s.%s WHERE username = '%s'", Auth.AUTH_KS, CREDENTIALS_CF, escape(username)),
//                consistencyForUser(username));
//=======
//>>>>>>> 223d0e755ee0480316f90621ac6389e942c23d97
    }

    public Set<DataResource> protectedResources()
    {
        // Also protected by CassandraRoleManager, but the duplication doesn't hurt and is more explicit
        return ImmutableSet.of(DataResource.table(AuthKeyspace.NAME, AuthKeyspace.ROLES));
    }

    public void validateConfiguration() throws ConfigurationException
    {
    }

    public void setup() //由Auth.setup()触发
    {
//<<<<<<< HEAD
//        Auth.setupTable(CREDENTIALS_CF, CREDENTIALS_CF_SCHEMA); //创建system_auth.credentials表
//
//        // the delay is here to give the node some time to see its peers - to reduce
//        // "skipped default user setup: some nodes are were not ready" log spam.
//        // It's the only reason for the delay.
//        //见org.apache.cassandra.auth.Auth.setup()的对应注
//        ScheduledExecutors.nonPeriodicTasks.schedule(new Runnable()
//        {
//            public void run()
//            {
//              setupDefaultUser(); //创建默认超级用户cassandra/cassandra
//            }
//        }, Auth.SUPERUSER_SETUP_DELAY, TimeUnit.MILLISECONDS);
//
//        try
//        {
//            String query = String.format("SELECT %s FROM %s.%s WHERE username = ?",
//                                         SALTED_HASH,
//                                         Auth.AUTH_KS,
//                                         CREDENTIALS_CF);
//            authenticateStatement = (SelectStatement) QueryProcessor.parseStatement(query).prepare().statement;
//        }
//        catch (RequestValidationException e)
//=======
        String query = String.format("SELECT %s FROM %s.%s WHERE role = ?",
                                     SALTED_HASH,
                                     AuthKeyspace.NAME,
                                     AuthKeyspace.ROLES);
        authenticateStatement = prepare(query);

        if (Schema.instance.getCFMetaData(AuthKeyspace.NAME, LEGACY_CREDENTIALS_TABLE) != null)
        {
            query = String.format("SELECT %s from %s.%s WHERE username = ?",
                                  SALTED_HASH,
                                  AuthKeyspace.NAME,
                                  LEGACY_CREDENTIALS_TABLE);
            legacyAuthenticateStatement = prepare(query);
        }
    }

    public AuthenticatedUser legacyAuthenticate(Map<String, String> credentials) throws AuthenticationException
    {
        String username = credentials.get(USERNAME_KEY);
        if (username == null)
            throw new AuthenticationException(String.format("Required key '%s' is missing", USERNAME_KEY));

        String password = credentials.get(PASSWORD_KEY);
        if (password == null)
            throw new AuthenticationException(String.format("Required key '%s' is missing", PASSWORD_KEY));

        return authenticate(username, password);
    }

    public SaslNegotiator newSaslNegotiator()
    {
//<<<<<<< HEAD
//        //见https://code.google.com/p/jbcrypt/
//        return BCrypt.hashpw(password, BCrypt.gensalt(GENSALT_LOG2_ROUNDS));
//=======
        return new PlainTextSaslAuthenticator();
    }

    private AuthenticatedUser doAuthenticate(String username, String password, SelectStatement authenticationStatement)
    throws RequestExecutionException, AuthenticationException
    {
        ResultMessage.Rows rows = authenticationStatement.execute(QueryState.forInternalCalls(),
                                                                  QueryOptions.forInternalCalls(consistencyForRole(username),
                                                                                                Lists.newArrayList(ByteBufferUtil.bytes(username))));
        UntypedResultSet result = UntypedResultSet.create(rows.result);

        if ((result.isEmpty() || !result.one().has(SALTED_HASH)) || !BCrypt.checkpw(password, result.one().getString(SALTED_HASH)))
            throw new AuthenticationException("Username and/or password are incorrect");

        return new AuthenticatedUser(username);
    }

    private SelectStatement prepare(String query)
    {
        return (SelectStatement) QueryProcessor.getStatement(query, ClientState.forInternalCalls()).statement;
    }

    private class PlainTextSaslAuthenticator implements SaslNegotiator
    {
        private boolean complete = false;
        private String username;
        private String password;

//<<<<<<< HEAD
//        //在org.apache.cassandra.transport.messages.AuthResponse.execute(QueryState)调用
//        @Override
//=======
//>>>>>>> 223d0e755ee0480316f90621ac6389e942c23d97
        public byte[] evaluateResponse(byte[] clientResponse) throws AuthenticationException
        {
            decodeCredentials(clientResponse);
            complete = true;
            return null;
        }

        public boolean isComplete()
        {
            return complete;
        }

//<<<<<<< HEAD
//        //验证用户名和密码是否正确
//        @Override
//=======
//>>>>>>> 223d0e755ee0480316f90621ac6389e942c23d97
        public AuthenticatedUser getAuthenticatedUser() throws AuthenticationException
        {
            if (!complete)
                throw new AuthenticationException("SASL negotiation not complete");
            return authenticate(username, password);
        }

        /**
         * SASL PLAIN mechanism specifies that credentials are encoded in a
         * sequence of UTF-8 bytes, delimited by 0 (US-ASCII NUL).
         * The form is : {code}authzId<NUL>authnId<NUL>password<NUL>{code}
         * authzId is optional, and in fact we don't care about it here as we'll
         * set the authzId to match the authnId (that is, there is no concept of
         * a user being authorized to act on behalf of another with this IAuthenticator).
         *
         * @param bytes encoded credentials string sent by the client
         * @return map containing the username/password pairs in the form an IAuthenticator
         * would expect
         * @throws javax.security.sasl.SaslException
         */
//<<<<<<< HEAD
//        //编码方式见com.datastax.driver.core.PlainTextAuthProvider.PlainTextAuthenticator.initialResponse()
//        //在org.apache.cassandra.transport.Client.encodeCredentialsForSasl(Map<String, String>)也有
//        private Map<String, String> decodeCredentials(byte[] bytes) throws AuthenticationException
//=======
        private void decodeCredentials(byte[] bytes) throws AuthenticationException
        {
            logger.debug("Decoding credentials from client token");
            byte[] user = null;
            byte[] pass = null;
            int end = bytes.length;
            //从后往前遍历，第一个看到的是密码，然后是用户名
            for (int i = bytes.length - 1 ; i >= 0; i--)
            {
                if (bytes[i] == NUL)
                {
                    if (pass == null)
                        pass = Arrays.copyOfRange(bytes, i + 1, end);
                    else if (user == null)
                        user = Arrays.copyOfRange(bytes, i + 1, end);
                    end = i;
                }
            }

            if (user == null)
                throw new AuthenticationException("Authentication ID must not be null");
            if (pass == null)
                throw new AuthenticationException("Password must not be null");

            username = new String(user, StandardCharsets.UTF_8);
            password = new String(pass, StandardCharsets.UTF_8);
        }
    }
}
