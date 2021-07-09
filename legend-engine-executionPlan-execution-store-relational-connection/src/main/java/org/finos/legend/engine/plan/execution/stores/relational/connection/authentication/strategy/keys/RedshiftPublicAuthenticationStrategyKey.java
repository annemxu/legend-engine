// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys;

import java.util.Objects;

public class RedshiftPublicAuthenticationStrategyKey implements AuthenticationStrategyKey
{
    private final String userName;
    private final String password;

    public RedshiftPublicAuthenticationStrategyKey(String userName, String password)
    {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        RedshiftPublicAuthenticationStrategyKey that = (RedshiftPublicAuthenticationStrategyKey) o;
        return Objects.equals(userName, that.userName) &&
                Objects.equals(password, that.password);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(userName, password);
    }

    @Override
    public String shortId()
    {
        return "type:" + type() +
                "_userName:" + userName +
                "_password:" + password;
    }

    @Override
    public String type()
    {
        return "RedshiftPublic";
    }
}
