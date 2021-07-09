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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;

import java.util.Objects;

public class RedshiftDataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String clusterName;
    private final String clusterID;
    private final String region;
    private final int port;
    private final String databaseName;


    public RedshiftDataSourceSpecificationKey(String clusterName, String clusterID, String region, int port, String databaseName)
    {
        this.clusterName = clusterName;
        this.clusterID = clusterID;
        this.region = region;
        this.port = port;
        this.databaseName = databaseName;
    }

    public String getClusterName()
    {
        return clusterName;
    }

    public String getClusterID()
    {
        return clusterID;
    }

    public String getRegion()
    {
        return region;
    }

    public int getPort()
    {
        return port;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    @Override
    public String toString()
    {
        return "RedshiftDataSourceSpecificationKey{" +
                "clusterName='" + clusterName + '\'' +
                ", clusterID='" + clusterID + '\'' +
                ", region='" + region + '\'' +
                ", port='" + port + '\'' +
                ", databaseName='" + databaseName + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Redshift_" +
                "clusterName:" + clusterName + "_" +
                "clusterID:" + clusterID + "_" +
                "region:" + region + "_" +
                "port:" + port + "_" +
                "databaseName:" + databaseName;
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
        RedshiftDataSourceSpecificationKey that = (RedshiftDataSourceSpecificationKey) o;
        return Objects.equals(clusterName, that.clusterName) &&
                Objects.equals(clusterID, that.clusterID) &&
                Objects.equals(region, that.region) &&
                Objects.equals(port, that.port) &&
                Objects.equals(databaseName, that.databaseName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(clusterName, clusterID, region, port, databaseName);
    }
}
