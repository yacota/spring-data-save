/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo;

import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoDataAutoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Defeult behaviour of SDM
 * @author jllach
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@EnableMongoRepositories
@TestPropertySource(inheritProperties = false, locations  = {"classpath:/application.properties"})
@ContextConfiguration(classes = {MongoEmbeddedConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class SpringNoChangeTest {
    
    @Autowired
    MongoTemplate template;
    
    @Test
    public void testOne() {
        One one = new One("one", "title one", "desc one");
        template.save(one);

        One foundOne = template.findOne(Query.query(Criteria.where("id").is(one.id)), One.class);
        Assert.assertThat(foundOne.title, Matchers.nullValue()); // because DBObjectAccessor overwrites the entire DATA structure, removing previous values
        Assert.assertThat(foundOne.desc,  Matchers.is("desc one"));
    }

    @Test
    public void testTwoWithoutMap() {
        Two two = new Two("two", "title two", "desc two", null);
        template.save(two);

        Two foundTwo = template.findOne(Query.query(Criteria.where("id").is(two.id)), Two.class);
        Assert.assertThat(foundTwo.title, Matchers.nullValue());
        Assert.assertThat(foundTwo.desc,  Matchers.is("desc two"));
        Assert.assertThat(foundTwo.map.get("TITLE"),  Matchers.nullValue());
        Assert.assertThat(foundTwo.map.get("DESC"),  Matchers.is("desc two"));
    }
    
    @Test
    public void testTwoWithMap() {
        Map values = new HashMap();
        values.put("TITLE", "title two from map wins");
        Two two = new Two("two2", "title two", "desc two", values);
        template.save(two);

        Two foundTwo = template.findOne(Query.query(Criteria.where("id").is(two.id)), Two.class);
        Assert.assertThat(foundTwo.title, Matchers.is("title two from map wins"));
        Assert.assertThat(foundTwo.desc, Matchers.nullValue());
        Assert.assertThat((String)foundTwo.map.get("TITLE"),  Matchers.is("title two from map wins"));
        Assert.assertThat((String)foundTwo.map.get("DESC"),  Matchers.nullValue());
    }

    @Test
    public void testThreeWithMap() {
        Map values = new HashMap();
        values.put("TITLE", "title three from map loses");
        values.put("DESC",  "desc three from map loses");
        Three three = new Three("three", "title three", "desc three", values);
        template.save(three);

        Three foundThree = template.findOne(Query.query(Criteria.where("id").is(three.id)), Three.class);
        Assert.assertThat(foundThree.title, Matchers.nullValue());
        Assert.assertThat(foundThree.desc, Matchers.is("desc three"));
        Assert.assertThat((String)foundThree.map.get("TITLE"),  Matchers.nullValue());
        Assert.assertThat((String)foundThree.map.get("DESC"), Matchers.is("desc three"));
    }
}