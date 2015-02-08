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

import java.util.Collections;
import java.util.Set;

import org.apache.cassandra.exceptions.InvalidRequestException;

//配置使用AllowAllAuthorizer时，不支持GRANT/REVOKE/LIST PERMISSIONS语句
public class AllowAllAuthorizer implements IAuthorizer
{
    public Set<Permission> authorize(AuthenticatedUser user, IResource resource)
    {
        return Permission.ALL;
    }

    public void grant(AuthenticatedUser performer, Set<Permission> permissions, IResource resource, RoleResource to)
    throws InvalidRequestException
    {
        throw new InvalidRequestException("GRANT operation is not supported by AllowAllAuthorizer");
    }

    public void revoke(AuthenticatedUser performer, Set<Permission> permissions, IResource resource, RoleResource from)
    throws InvalidRequestException
    {
        throw new InvalidRequestException("REVOKE operation is not supported by AllowAllAuthorizer");
    }

    public void revokeAllFrom(RoleResource droppedRole)
    {
    }

    public void revokeAllOn(IResource droppedResource)
    {
    }

    public Set<PermissionDetails> list(AuthenticatedUser performer, Set<Permission> permissions, IResource resource, RoleResource of)
    throws InvalidRequestException
    {
        throw new InvalidRequestException("LIST PERMISSIONS operation is not supported by AllowAllAuthorizer");
    }

    public Set<IResource> protectedResources()
    {
        return Collections.emptySet();
    }

    public void validateConfiguration()
    {
    }

    public void setup()
    {
    }
}
