/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.springframework.data.mongodb.core.convert;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.Arrays;
import java.util.Iterator;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.util.Assert;

/**
 *
 * @author jllach
 */
public class CustomDBObjectAccessor 
extends      DBObjectAccessor {
    
    private final BasicDBObject dbObject;

    public CustomDBObjectAccessor(DBObject dbObject) {
        super(dbObject);
        this.dbObject = (BasicDBObject)dbObject;
    }
    
    @Override
    public void put(MongoPersistentProperty prop, Object value) {
        Assert.notNull(prop, "MongoPersistentProperty must not be null!");
        String fieldName = prop.getFieldName();
        if (!fieldName.contains(".")) {
            dbObject.put(fieldName, value);
            return;
        }

        Iterator<String> parts = Arrays.asList(fieldName.split("\\.")).iterator();
        DBObject dbObject = this.dbObject;

        while (parts.hasNext()) {

            String part = parts.next();

            if (parts.hasNext()) {
                BasicDBObject nestedDbObject;
                Object previous = dbObject.get(part); //NBA : checking if dbObject already holds an instance with "part" name and just reusing it in case is a BasicDBObject
                if (previous != null && previous instanceof BasicDBObject) {
                    nestedDbObject = (BasicDBObject)previous;
                } else {
                    nestedDbObject = new BasicDBObject();
                }
                dbObject.put(part, nestedDbObject);
                dbObject = nestedDbObject;
            } else {
                dbObject.put(part, value);
            }
        }
    }
}