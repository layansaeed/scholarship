package com.example.beans.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.configuration2.DatabaseConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.spring.ConfigurationPropertiesFactoryBean;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class Config {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Configuration
    public static class PropertySourceConfig extends PropertySourcesPlaceholderConfigurer {

        private static final Logger log = LoggerFactory.getLogger(PropertySourceConfig.class);

        private static final String SPRING_DATASOURCE_DRIVER = "spring.datasource.driver-class-name";
        private static final String SPRING_DATASOURCE_URL = "spring.datasource.url";
        private static final String SPRING_DATASOURCE_USERNAME = "spring.datasource.username";
        private static final String SPRING_DATASOURCE_PASSWORD = "spring.datasource.password";

        private static final String CONFIG_TABLE_SCHEMA = "config.table.schema";
        private static final String CONFIG_TABLE_NAME = "config.table.name";
        private static final String CONFIG_TABLE_KEY_COLUMN = "config.table.key-column";
        private static final String CONFIG_TABLE_VALUE_COLUMN = "config.table.value-column";

        private Environment env;
        private String[] loadedPropertyNames = new String[0];

        @Bean
        public HikariDataSource propertyConfigDataSource() {
            return createPropertyConfigDataSource();
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            this.env = beanFactory.getBean(Environment.class);

            MutablePropertySources propertySources =
                    ((ConfigurableEnvironment) env).getPropertySources();

            try {
                Properties dbProperties = loadDatabaseProperties();

                PropertiesPropertySource dbPropertySource =
                        new PropertiesPropertySource("dbPropertySource", dbProperties);

                propertySources.addFirst(dbPropertySource);
                this.loadedPropertyNames = dbPropertySource.getPropertyNames();

                log.info("Loaded {} properties from {}", loadedPropertyNames.length, getConfigTableFullName());

            } catch (Exception e) {
                log.error("Failed to load DB properties from {}", getConfigTableFullName(), e);
                throw new IllegalStateException("Failed to load DB configuration properties", e);
            }

            super.postProcessBeanFactory(beanFactory);
        }

        private Properties loadDatabaseProperties() throws ConfigurationException {
            HikariDataSource dataSource = createPropertyConfigDataSource();

            try {
                BasicConfigurationBuilder<DatabaseConfiguration> builder =
                        new BasicConfigurationBuilder<>(DatabaseConfiguration.class);

                builder.configure(new Parameters().database()
                        .setDataSource(dataSource)
                        .setTable(getConfigTableFullName())
                        .setKeyColumn(getRequiredEnvironmentValue(CONFIG_TABLE_KEY_COLUMN))
                        .setValueColumn(getRequiredEnvironmentValue(CONFIG_TABLE_VALUE_COLUMN)));

                DatabaseConfiguration databaseConfiguration = builder.getConfiguration();

                ConfigurationPropertiesFactoryBean factoryBean = new ConfigurationPropertiesFactoryBean();
                factoryBean.setConfigurations(databaseConfiguration);

                try {
                    factoryBean.afterPropertiesSet();
                    Properties dbProperties = factoryBean.getObject();
                    return dbProperties != null ? dbProperties : new Properties();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to convert database configuration to Properties", e);
                }

            } finally {
                dataSource.close();
            }
        }

        private HikariDataSource createPropertyConfigDataSource() {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setDriverClassName(getRequiredEnvironmentValue(SPRING_DATASOURCE_DRIVER));
            dataSource.setJdbcUrl(getRequiredEnvironmentValue(SPRING_DATASOURCE_URL));
            dataSource.setUsername(getRequiredEnvironmentValue(SPRING_DATASOURCE_USERNAME));
            dataSource.setPassword(getRequiredEnvironmentValue(SPRING_DATASOURCE_PASSWORD));
            return dataSource;
        }

        public Map<String, String> getPropertiesStartingWith(String prefix) {
            Map<String, String> matchingProperties = new LinkedHashMap<>();

            for (String propertyName : loadedPropertyNames) {
                if (propertyName.startsWith(prefix)) {
                    matchingProperties.put(propertyName, env.getProperty(propertyName));
                }
            }

            return matchingProperties;
        }

        private String getConfigTableFullName() {
            return getRequiredEnvironmentValue(CONFIG_TABLE_SCHEMA)
                    + "."
                    + getRequiredEnvironmentValue(CONFIG_TABLE_NAME);
        }

        private String getRequiredEnvironmentValue(String key) {
            String value = env != null ? env.getProperty(key) : null;

            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Missing required environment property: " + key);
            }

            return value;
        }
    }
}