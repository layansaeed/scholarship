package com.example.beans.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.configuration.DatabaseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.web.client.RestTemplate;
import org.springmodules.commons.configuration.CommonsConfigurationFactoryBean;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class Config {

    @Bean
   public RestTemplate restTemplate()
    {
        return new RestTemplate();
    }

    /**
     * This is the core class that loads DB rows as Spring properties then add them into Environment.
     * Before any request comes in, PropertySourceConfig runs.
     * It:
     * connects to DB
     * reads all rows from EXT.JOB_CONFIG
     * converts them into Spring properties
     * adds them to Spring Environment
     */
    @Configuration
    public static class PropertySourceConfig extends PropertySourcesPlaceholderConfigurer {

        private static final Logger log = LoggerFactory.getLogger(PropertySourceConfig.class);

        //in properties file
        private static final String SPRING_DATASOURCE_DRIVER = "spring.datasource.driver-class-name";
        private static final String SPRING_DATASOURCE_URL = "spring.datasource.url";
        private static final String SPRING_DATASOURCE_USERNAME = "spring.datasource.username";
        private static final String SPRING_DATASOURCE_PASSWORD = "spring.datasource.password";

        private static final String CONFIG_TABLE_SCHEMA = "config.table.schema";
        private static final String CONFIG_TABLE_NAME = "config.table.name";
        private static final String CONFIG_TABLE_KEY_COLUMN = "config.table.key-column";
        private static final String CONFIG_TABLE_VALUE_COLUMN = "config.table.value-column";

        //store all properties from anywhere (yaml/DB/property)
        private Environment env;
        private String[] loadedPropertyNames = new String[0];

        //create connect with DB -> datasource object using normal datasource values:
        @Bean
        public HikariDataSource propertyConfigDataSource() {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setDriverClassName(getRequiredEnvironmentValue(SPRING_DATASOURCE_DRIVER));
            dataSource.setJdbcUrl(getRequiredEnvironmentValue(SPRING_DATASOURCE_URL));
            dataSource.setUsername(getRequiredEnvironmentValue(SPRING_DATASOURCE_USERNAME));
            dataSource.setPassword(getRequiredEnvironmentValue(SPRING_DATASOURCE_PASSWORD));
            return dataSource;
        }

        /**
         * Runs early during Spring startup.
         *
         * Purpose:
         * - read key/value rows from DB config table
         * - convert them into normal Spring properties
         * - add them to Spring Environment
         *
         * Example:
         * DB row:
         * HRSD_DIS_ASS.url = http://localhost:3000/hrsd-dis-serv
         *
         * After this method runs, Spring can read:
         * env.getProperty("HRSD_DIS_ASS.url")
         */
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

            //Get the current Spring Environment is where Spring stores properties.
            //Right now it already contains: application.properties & system env variables & JVM properties
            this.env = beanFactory.getBean(Environment.class);
            //The list/container of all property sources Spring knows about -> I want to add DB properties into it and edit  so mutable
            MutablePropertySources propertySources =
                    ((ConfigurableEnvironment) env).getPropertySources();

            try {
                //connect to DB and read key/value properties from this table
                //load driver -> open connection -> prepare SQL ->read every config row from the table
                //DatabaseConfiguration: represent configuration read from the database
                DatabaseConfiguration databaseConfiguration = new DatabaseConfiguration(
                        propertyConfigDataSource(),
                        getConfigTableFullName(),
                        getRequiredEnvironmentValue(CONFIG_TABLE_KEY_COLUMN),
                        getRequiredEnvironmentValue(CONFIG_TABLE_VALUE_COLUMN)
                );
//                DatabaseConfiguration databaseConfiguration = new DatabaseConfiguration(
//                        propertyConfigDataSource(),
//                        getQuotedConfigTableFullName(),
//                        getQuotedIdentifierValue(CONFIG_TABLE_KEY_COLUMN),
//                        getQuotedIdentifierValue(CONFIG_TABLE_VALUE_COLUMN)
//                );

                //#The rows from DB are converted into a normal Java Properties object to behave like a .properties file in memory
                //Above I Created a Commons DB configuration object
                //1.Wrap it in a Spring factory bean
                CommonsConfigurationFactoryBean factoryBean =
                        new CommonsConfigurationFactoryBean(databaseConfiguration);
                //2. Initialize it
                factoryBean.afterPropertiesSet();
                //3.Ask it for the object it created
                Properties dbProperties = (Properties) factoryBean.getObject();

                if (dbProperties == null) {
                    dbProperties = new Properties();
                }

                //convert raw properties(java object) into something Spring Environment understands
                // so wrap those properties as a Spring PropertySource Because Spring works with PropertySource, not just raw Properties.
                PropertiesPropertySource dbPropertySource =
                        new PropertiesPropertySource("dbPropertySource", dbProperties);

                //Now the DB properties are inserted into Spring’s list of property sources.
                //rest of the application can use those values like normal properties.
                propertySources.addFirst(dbPropertySource);
                //Save all loaded keys in an array Because later we want to search by prefix.
                this.loadedPropertyNames = dbPropertySource.getPropertyNames();

                log.info("Loaded {} properties from {}", loadedPropertyNames.length,getConfigTableFullName());

            } catch (Exception e) {
                log.error("Failed to load DB properties from {}", getConfigTableFullName(), e);
                throw new IllegalStateException("Failed to load DB configuration properties", e);
            }

            //Now that DB properties are added, tell Spring: continue standard Spring processing with the updated environment
            super.postProcessBeanFactory(beanFactory);

            /**
             * At the end
             * Environment contains DB config keys
             * normal services can now access them by using env
             * So later, when the code needs config for a job, it does not query DB with JPA using repo.
             * Instead, it reads from the already-loaded environment-backed property source.
             * This is the key architectural change.
             */
        }

        /**
         * Returns all loaded properties whose keys start with the given prefix.
         * collect all config that belongs to one job.
         * Example:
         * prefix = "HRSD_DIS_ASS."
         *
         * Result may include:
         * - HRSD_DIS_ASS.url
         * - HRSD_DIS_ASS.httpMethod
         * - HRSD_DIS_ASS.auth.url
         */
        public Map<String, String> getPropertiesStartingWith(String prefix) {
            Map<String, String> matchingProperties = new LinkedHashMap<String, String>();

            for (String propertyName : loadedPropertyNames) {
                if (propertyName.startsWith(prefix)) {
                    String value = env.getProperty(propertyName);
                    matchingProperties.put(propertyName, value);
                }
            }

            return matchingProperties;
        }
    //--------------------------------------------------------------

        private String getQuotedConfigTableFullName() {
            return quoteIdentifier(getRequiredEnvironmentValue(CONFIG_TABLE_SCHEMA))
                    + "."
                    + quoteIdentifier(getRequiredEnvironmentValue(CONFIG_TABLE_NAME));
        }

        private String getQuotedIdentifierValue(String propertyKey) {
            return quoteIdentifier(getRequiredEnvironmentValue(propertyKey));
        }

        private String quoteIdentifier(String value) {
            String trimmed = value.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                return trimmed;
            }
            return "[" + trimmed + "]";
        }

    //--------------------------------------------------------------
        private String getConfigTableFullName() {
            return getRequiredEnvironmentValue(CONFIG_TABLE_SCHEMA) + "."
                    + getRequiredEnvironmentValue(CONFIG_TABLE_NAME);
        }

        private String getRequiredEnvironmentValue(String key) {
            String value = env != null ? env.getProperty(key) : null;

            if (value == null || value.trim().isEmpty()) {
                throw new IllegalStateException("Missing required environment property: " + key);
            }

            return value;
        }
    }
}

