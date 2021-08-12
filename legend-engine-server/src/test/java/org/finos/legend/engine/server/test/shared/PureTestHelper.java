// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.server.test.shared;

import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.UserPasswordAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.RedshiftDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.RedshiftDataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.RelationalConnectionManager;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.server.Server;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.pure.configuration.PureRepositoriesExternal;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.VersionControlledClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.Revision;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;
import org.junit.Assert;
import org.junit.Ignore;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.Objects;
import java.util.Properties;

public class PureTestHelper
{
    private static final ThreadLocal<ServersState> state = new ThreadLocal<>();

    private static RelationalDatabaseConnection redshifttest;

    public static boolean initClientVersionIfNotAlreadySet(String defaultClientVersion)
    {
        boolean isNotSet = System.getProperty("alloy.test.clientVersion") == null && System.getProperty("legend.test.clientVersion") == null;
        if (isNotSet)
        {
            System.setProperty("alloy.test.clientVersion", defaultClientVersion);
            System.setProperty("legend.test.clientVersion", defaultClientVersion);
            System.setProperty("alloy.test.serverVersion", "v1");
            System.setProperty("legend.test.serverVersion", "v1");
        }
        return isNotSet;
    }

    public static void cleanUpRedshiftTables() {

        RelationalConnectionManager redshiftManager = new RelationalConnectionManager(22, Lists.mutable.empty(), ConcurrentHashMap.newMap(), new RelationalExecutorInfo());
        String redshiftConnectionStr =
                "{\n" +
                        "  \"_type\": \"RelationalDatabaseConnection\",\n" +
                        "  \"type\": \"Redshift\",\n" +
                        "  \"authenticationStrategy\" : {\n" +
                        "    \"_type\" : \"userPassword\",\n" +
                        "    \"userName\" : \"awsuser\",\n" +
                        "    \"passwordVaultReference\" : \"08r3EB28DF9B8757AAD75A6F8FC20B7713EF!\"\n" +
                        "  },\n" +
                        "  \"datasourceSpecification\" : {\n" +
                        "    \"_type\" : \"redshift\",\n" +
                        "    \"databaseName\" : \"dev\",\n" +
                        "    \"endpoint\" : \"lab.cqzp3tj1qpzo.us-east-2.redshift.amazonaws.com\",\n" +
                        "    \"port\" : \"5439\"\n" +
                        "  }\n" +
                        "}";




//        RelationalDatabaseConnection connectionSpec = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(redshiftConnectionStr, RelationalDatabaseConnection.class);
        try(Connection connection = redshiftManager.getDataSourceSpecification(redshifttest).getConnectionUsingSubject(null)) {
//            try(Connection connection = redshiftManager.getDataSourceSpecification(connectionSpec).getConnectionUsingSubject(null)) {

                try (Statement statement = connection.createStatement()) {
                statement.execute("Drop table if exists PersonNameParameterTest;");
                statement.execute("Drop table if exists dataTable;");
                statement.execute("Drop table if exists tradeTable;");
                System.out.println("Cleanup finished");

            }
        }
        catch(Error | SQLException e)
        {
            System.out.println("Error");
        }

    }
    public static void cleanUp()
    {


        System.clearProperty("alloy.test.clientVersion");
        System.clearProperty("legend.test.clientVersion");
    }

    @Ignore
    public static TestSetup wrapSuite(Function0<Boolean> init, Function0<TestSuite> suiteBuilder){
        boolean shouldCleanUp = init.value();
        TestSuite suite = suiteBuilder.value();
        if (shouldCleanUp)
        {

            cleanUp();
        }
        return new TestSetup(suite)
        {
            boolean shouldCleanUp;
            @Override
            protected void setUp() throws Exception
            {
                super.setUp();
                shouldCleanUp = init.value();
                state.set(initEnvironment());
            }

            @Override
            protected void tearDown() throws Exception
            {
                super.tearDown();
                state.get().shutDown();
                state.remove();
                System.out.println("Starting cleanuptest");
                cleanUpRedshiftTables();
                if (this.shouldCleanUp)
                {
                    cleanUp();
                }
                System.out.println("STOP");
            }
        };
    }

    public static ServersState initEnvironment() throws Exception
    {
        int engineServerPort = 1100 + (int) (Math.random() * 30000);
        int metadataServerPort = 1100 + (int) (Math.random() * 30000);
        int relationalDBPort = 1100 + (int) (Math.random() * 30000);


        //qwerty test
        RelationalConnectionManager redshiftManager = new RelationalConnectionManager(22, Lists.mutable.empty(), ConcurrentHashMap.newMap(), new RelationalExecutorInfo());

        String redshiftConnectionStr =
                "{\n" +
                        "  \"_type\": \"RelationalDatabaseConnection\",\n" +
                        "  \"type\": \"Redshift\",\n" +
                        "  \"authenticationStrategy\" : {\n" +
                        "    \"_type\" : \"userPassword\",\n" +
                        "    \"userName\" : \"awsuser\",\n" +
                        "    \"passwordVaultReference\" : \"08r3EB28DF9B8757AAD75A6F8FC20B7713EF!\"\n" +
                        "  },\n" +
                        "  \"datasourceSpecification\" : {\n" +
                        "    \"_type\" : \"redshift\",\n" +
                        "    \"databaseName\" : \"dev\",\n" +
                        "    \"endpoint\" : \"lab.cqzp3tj1qpzo.us-east-2.redshift.amazonaws.com\",\n" +
                        "    \"port\" : \"5439\"\n" +
                        "  }\n" +
                        "}";

        RelationalDatabaseConnection connectionSpec = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(redshiftConnectionStr, RelationalDatabaseConnection.class);
        redshifttest = connectionSpec;
        try(Connection connection = redshiftManager.getDataSourceSpecification(connectionSpec).getConnectionUsingSubject(null)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("Drop table if exists PersonNameParameterTest;");
                statement.execute("Create Table PersonNameParameterTest(id INT, lastNameFirst VARCHAR(200), title VARCHAR(200));");
                statement.execute("insert into PersonNameParameterTest (id, lastNameFirst, title) values (1, \'true\', \'eee\');");
                statement.execute("Drop table if exists dataTable;");
                statement.execute("Create Table dataTable( int1 INTEGER PRIMARY KEY,string1 VARCHAR(200),string2 VARCHAR(200),string3 VARCHAR(200),string2float  VARCHAR(200),string2Decimal VARCHAR(200),string2Integer  VARCHAR(200),string2date  VARCHAR(200),stringDateFormat VARCHAR(12),stringDateTimeFormat VARCHAR(32),dateTime TIMESTAMP,float1 Float,stringUserDefinedDateFormat VARCHAR(7),stringToInt VARCHAR(5),alphaNumericString VARCHAR(15));");
                statement.execute("insert into dataTable (int1, string1, string2, string3, dateTime, float1,string2float,string2Decimal,string2date,stringDateFormat,stringDateTimeFormat,stringUserDefinedDateFormat, stringToInt, alphaNumericString ) values (1, \'Joe\', \' Bloggs \', 10, \'2003-07-19 05:00:00\', 1.1,\'123.456\',\'123.450021\', \'2016-06-23 00:00:00.123\',\'2016-06-23\',\'2016-06-23 13:00:00.123\', \'NOV1995\', \'33\', \'loremipsum33\' )");
                statement.execute("insert into dataTable (int1, string1, string2, string3, dateTime, float1,string2float,string2Decimal,string2date,stringDateFormat,stringDateTimeFormat,stringUserDefinedDateFormat, stringToInt, alphaNumericString ) values (2, \'Mrs\', \'Smith\', 11, \'2003-07-20 02:00:00\', 1.8,\'100.001\', \'0100.009\',\'2016-06-23 00:00:00.345\',\'2016-02-23\',\'2016-02-23 23:00:00.1345\', \'NOV1995\', \'42\', \'lorem42ipsum\')");
                statement.execute("Drop table if exists tradeTable;");
                statement.execute("Create Table tradeTable(id INT, prodid INT, accountId INT, quantity FLOAT, tradeDate DATE, settlementDateTime TIMESTAMP);");
                statement.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (1, 1, 1, 25, \'2014-12-01\', \'2014-12-02 21:00:00\');");
                statement.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (2, 1, 2, 320, \'2014-12-01\',\'2014-12-02 21:00:00\');");
                statement.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (3, 2, 1, 11, \'2014-12-01\', \'2014-12-02 21:00:00\');");
                statement.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (4, 2, 2, 23, \'2014-12-02\', \'2014-12-03 21:00:00\');");
                statement.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (5, 2, 1, 32, \'2014-12-02\', \'2014-12-03 21:00:00\');");
                statement.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (6, 3, 1, 27, \'2014-12-03\', \'2014-12-04 21:00:00\');");
                statement.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (7, 3, 1, 44, \'2014-12-03\', \'2014-12-04 15:22:23.123456789\');");
                statement.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (8, 3, 2, 22, \'2014-12-04\', \'2014-12-05 21:00:00\');");
                statement.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (9, 3, 2, 45, \'2014-12-04\', \'2014-12-05 21:00:00\');");
                statement.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate) values (10, 3, 2, 38, \'2014-12-04\');");
                System.out.println("Setup finished");

            }
        }
        //qwerty end
        // Relational



        org.h2.tools.Server h2Server = AlloyH2Server.startServer(relationalDBPort);
        System.out.println("H2 database started on port:" + relationalDBPort);


        // Start metadata server
        TestMetaDataServer metadataServer = new TestMetaDataServer(metadataServerPort, true);
        System.out.println("Metadata server started on port:" + metadataServerPort);

        // Start engine server
        System.setProperty("dw.server.connector.port", String.valueOf(engineServerPort));
        System.setProperty("dw.metadataserver.pure.port", String.valueOf(metadataServerPort));
        System.setProperty("dw.temporarytestdb.port", String.valueOf(relationalDBPort));
        System.out.println("Found Config file: " + Objects.requireNonNull(PureTestHelper.class.getClassLoader().getResource("org/finos/legend/engine/server/test/userTestConfig.json")).getFile());

        Server server = new Server();
        server.run("server", Objects.requireNonNull(PureTestHelper.class.getClassLoader().getResource("org/finos/legend/engine/server/test/userTestConfig.json")).getFile());
        System.out.println("Alloy server started on port:" + engineServerPort);

        // Pure client configuration (to call the engine server)
        System.setProperty("test.metadataserver.pure.port", String.valueOf(metadataServerPort));
        System.setProperty("alloy.test.h2.port", String.valueOf(relationalDBPort));
        System.setProperty("legend.test.h2.port", String.valueOf(relationalDBPort));
        System.setProperty("alloy.test.server.host", "127.0.0.1");
        System.setProperty("alloy.test.server.port", String.valueOf(engineServerPort));
        System.setProperty("legend.test.server.host", "127.0.0.1");
        System.setProperty("legend.test.server.port", String.valueOf(engineServerPort));
        System.out.println("Pure client configured to reach engine server");

        return new ServersState(null, null , null);

//        return new ServersState(server, metadataServer, h2Server);
    }

    private static boolean hasTestStereotypeWithValue(CoreInstance node, String value, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(node, M3Properties.stereotypes, processorSupport);
        if (stereotypes.notEmpty())
        {
            CoreInstance testProfile = processorSupport.package_getByUserPath("meta::pure::profiles::test");
            for (CoreInstance stereotype : stereotypes)
            {
                if ((testProfile == Instance.getValueForMetaPropertyToOneResolved(stereotype, M3Properties.profile, processorSupport)) &&
                        value.equals(Instance.getValueForMetaPropertyToOneResolved(stereotype, M3Properties.value, processorSupport).getName()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean shouldExcludeOnClientVersion(CoreInstance node, String serverVersion, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> taggedValues = Instance.getValueForMetaPropertyToManyResolved(node, M3Properties.taggedValues, processorSupport);
        if (taggedValues.notEmpty())
        {
            CoreInstance serverVersionProfile = processorSupport.package_getByUserPath("meta::pure::executionPlan::profiles::serverVersion");
            for (CoreInstance taggedValue : taggedValues)
            {
                if ((taggedValue.getValueForMetaPropertyToOne("tag").getValueForMetaPropertyToOne("profile") == serverVersionProfile)
                        && (taggedValue.getValueForMetaPropertyToOne("tag").getName().equals("exclude")) &&
                        serverVersion.toLowerCase().equals(Instance.getValueForMetaPropertyToOneResolved(taggedValue, M3Properties.value, processorSupport).getName().toLowerCase()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean shouldExecuteOnClientVersionOnwards(CoreInstance node, String serverVersion, ProcessorSupport processorSupport)
    {
        ListIterable<? extends CoreInstance> taggedValues = Instance.getValueForMetaPropertyToManyResolved(node, M3Properties.taggedValues, processorSupport);
        if (taggedValues.notEmpty())
        {
            CoreInstance serverVersionProfile = processorSupport.package_getByUserPath("meta::pure::executionPlan::profiles::serverVersion");
            for (CoreInstance taggedValue : taggedValues)
            {
                if ((taggedValue.getValueForMetaPropertyToOne("tag").getValueForMetaPropertyToOne("profile") == serverVersionProfile)
                        && (taggedValue.getValueForMetaPropertyToOne("tag").getName().equals("start")))
                {
                    return PureClientVersions.versionAGreaterThanOrEqualsVersionB(serverVersion.toLowerCase(), Instance.getValueForMetaPropertyToOneResolved(taggedValue, M3Properties.value, processorSupport).getName().toLowerCase());
                }

            }
        }
        return true;
    }

    public static boolean satisfiesConditions(CoreInstance node, ProcessorSupport processorSupport)
    {
        String ver = System.getProperty("alloy.test.clientVersion") == null ? System.getProperty("legend.test.clientVersion") : System.getProperty("alloy.test.clientVersion");
        return !hasTestStereotypeWithValue(node, "ExcludeAlloy", processorSupport) &&
                !shouldExcludeOnClientVersion(node, ver, processorSupport) &&
                shouldExecuteOnClientVersionOnwards(node, ver, processorSupport);
    }

    public static CompiledExecutionSupport getExecutionSupport()
    {
        return new CompiledExecutionSupport(
                new JavaCompilerState(null, PureTestHelper.class.getClassLoader()),
                new CompiledProcessorSupport(PureTestHelper.class.getClassLoader(), new MetadataLazy(PureTestHelper.class.getClassLoader()), Sets.mutable.empty()),
                null,
                new CodeStorage()
                {
                    @Override
                    public String getRepoName(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<String> getAllRepoNames()
                    {
                        return null;
                    }

                    @Override
                    public boolean isRepoName(String s)
                    {
                        return false;
                    }

                    @Override
                    public RichIterable<CodeRepository> getAllRepositories()
                    {
                        return null;
                    }

                    @Override
                    public CodeRepository getRepository(String s)
                    {
                        return null;
                    }

                    @Override
                    public CodeStorageNode getNode(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<CodeStorageNode> getFiles(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<String> getUserFiles()
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<String> getFileOrFiles(String s)
                    {
                        return null;
                    }

                    @Override
                    public InputStream getContent(String s)
                    {
                        return null;
                    }

                    @Override
                    public byte[] getContentAsBytes(String s)
                    {
                        return new byte[0];
                    }

                    @Override
                    public String getContentAsText(String s)
                    {
                        return null;
                    }

                    @Override
                    public boolean exists(String s)
                    {
                        return false;
                    }

                    @Override
                    public boolean isFile(String s)
                    {
                        return false;
                    }

                    @Override
                    public boolean isFolder(String s)
                    {
                        return false;
                    }

                    @Override
                    public boolean isEmptyFolder(String s)
                    {
                        return false;
                    }

                    @Override
                    public boolean isVersioned(String s)
                    {
                        return false;
                    }

                    @Override
                    public long getCurrentRevision(String s)
                    {
                        return 0;
                    }

                    @Override
                    public LongList getAllRevisions(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<Revision> getAllRevisionLogs(RichIterable<String> richIterable)
                    {
                        return null;
                    }
                },
                null,
                null,
                new ConsoleCompiled(),
                new FunctionCache(),
                new ClassCache(),
                null,
                Sets.mutable.empty()
        );
    }

    public static CompiledExecutionSupport getClassLoaderExecutionSupport()
    {
        ConsoleCompiled console = new ConsoleCompiled();
        console.disable();

        return new CompiledExecutionSupport(
                new JavaCompilerState(null, PureTestHelper.class.getClassLoader()),
                new CompiledProcessorSupport(PureTestHelper.class.getClassLoader(), new MetadataLazy(PureTestHelper.class.getClassLoader()), Sets.mutable.empty()),
                null,
                new PureCodeStorage(null, new VersionControlledClassLoaderCodeStorage(PureTestHelper.class.getClassLoader(), PureRepositoriesExternal.repositories(), null)),
                null,
                null,
                console,
                new FunctionCache(),
                new ClassCache(),
                null,
                Sets.mutable.empty()
        );
    }

    @Ignore
    public static class PureTestCase extends TestCase
    {
        CoreInstance coreInstance;
        ExecutionSupport executionSupport;

        public PureTestCase()
        {
        }

        PureTestCase(CoreInstance coreInstance, ExecutionSupport executionSupport)
        {
            super(coreInstance.getValueForMetaPropertyToOne("functionName").getName());
            this.coreInstance = coreInstance;
            this.executionSupport = executionSupport;
        }

        @Override
        protected void runTest() throws Throwable
        {
            Class<?> _class = Class.forName("org.finos.legend.pure.generated." + IdBuilder.sourceToId(coreInstance.getSourceInformation()));
            Method method = _class.getMethod(FunctionProcessor.functionNameToJava(coreInstance), ExecutionSupport.class);
            // NOTE: mock out the global tracer for test
            // See https://github.com/opentracing/opentracing-java/issues/170
            // See https://github.com/opentracing/opentracing-java/issues/364
            GlobalTracer.registerIfAbsent(NoopTracerFactory.create());
            String testName = PackageableElement.getUserPathForPackageableElement(this.coreInstance);
            System.out.print("EXECUTING " + testName + " ... ");
            long start = System.nanoTime();
            try
            {
//                System.out.println("qerty running test");
                method.invoke(null, this.executionSupport);
                System.out.format("DONE (%.6fs)\n", (System.nanoTime() - start) / 1_000_000_000.0);
            }
	    catch(InvocationTargetException e)
            {
                System.out.format("ERROR qwerty (%.6fs)\n", (System.nanoTime() - start) / 1_000_000_000.0);
                throw e.getTargetException();
            }
        }
    }

    public static TestSuite buildSuite(TestCollection testCollection, ExecutionSupport executionSupport)
    {
        MutableList<TestSuite> subSuites = new FastList<>();
        for (TestCollection collection : testCollection.getSubCollections().toSortedList(Comparator.comparing(a -> a.getPackage().getName())))
        {
            subSuites.add(buildSuite(collection, executionSupport));
        }
        return buildSuite(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.GET_USER_PATH.valueOf(testCollection.getPackage()),
                testCollection.getBeforeFunctions(),
                testCollection.getAfterFunctions(),
                testCollection.getPureAndAlloyOnlyFunctions(),
                subSuites,
                executionSupport
        );
    }

    private static TestSuite buildSuite(String packageName, RichIterable<CoreInstance> beforeFunctions, RichIterable<CoreInstance> afterFunctions,
                                        RichIterable<CoreInstance> testFunctions, org.eclipse.collections.api.list.ListIterable<TestSuite> subSuites, ExecutionSupport executionSupport)
    {
        TestSuite suite = new TestSuite();
        suite.setName(packageName);
        beforeFunctions.collect(fn -> new PureTestCase(fn, executionSupport)).each(suite::addTest);
        for (Test subSuite : subSuites.toSortedList(Comparator.comparing(TestSuite::getName)))
        {
            suite.addTest(subSuite);
        }
        for (CoreInstance testFunc : testFunctions.toSortedList(Comparator.comparing(CoreInstance::getName)))
        {
            Test theTest = new PureTestCase(testFunc, executionSupport);
            suite.addTest(theTest);
        }
        afterFunctions.collect(fn -> new PureTestCase(fn, executionSupport)).each(suite::addTest);
        return suite;
    }
}
