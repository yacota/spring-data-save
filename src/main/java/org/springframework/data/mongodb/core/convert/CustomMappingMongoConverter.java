/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.springframework.data.mongodb.core.convert;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.core.convert.ConversionException;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.CollectionUtils;

/**
 * Using CustomDbObjectAccessor instead of DBObjectAccessor, that's all
 */
public class CustomMappingMongoConverter 
extends      MappingMongoConverter {
    
    public CustomMappingMongoConverter(DbRefResolver dbRefResolver, MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
        super(dbRefResolver, mappingContext);
    }

    public CustomMappingMongoConverter(MongoDbFactory mongoDbFactory, MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
        super(mongoDbFactory, mappingContext);
    }
    
    @Override
    protected void writeInternal(Object obj, final DBObject dbo, MongoPersistentEntity<?> entity) {
        if (obj == null) {
			return;
		}

		if (null == entity) {
			throw new MappingException("No mapping metadata found for entity of type " + obj.getClass().getName());
		}

		final PersistentPropertyAccessor accessor = entity.getPropertyAccessor(obj);
		final MongoPersistentProperty idProperty = entity.getIdProperty();

		if (!dbo.containsField("_id") && null != idProperty) {

			try {
				Object id = accessor.getProperty(idProperty);
				dbo.put("_id", idMapper.convertId(id));
			} catch (ConversionException ignored) {}
		}

		// Write the properties
		entity.doWithProperties(new PropertyHandler<MongoPersistentProperty>() {
			public void doWithPersistentProperty(MongoPersistentProperty prop) {

				if (prop.equals(idProperty) || !prop.isWritable()) {
					return;
				}

				Object propertyObj = accessor.getProperty(prop);

				if (null != propertyObj) {

					if (!conversions.isSimpleType(propertyObj.getClass())) {
						writePropertyInternal(propertyObj, dbo, prop);
					} else {
						writeSimpleInternal(propertyObj, dbo, prop);
					}
				}
			}
		});

		entity.doWithAssociations(new AssociationHandler<MongoPersistentProperty>() {

			public void doWithAssociation(Association<MongoPersistentProperty> association) {

				MongoPersistentProperty inverseProp = association.getInverse();
				Object propertyObj = accessor.getProperty(inverseProp);

				if (null != propertyObj) {
					writePropertyInternal(propertyObj, dbo, inverseProp);
				}
			}
		});    
        
    }
    
    @Override
    @SuppressWarnings({ "unchecked" })
	protected void writePropertyInternal(Object obj, DBObject dbo, MongoPersistentProperty prop) {
		if (obj == null) {
			return;
		}
		CustomDBObjectAccessor accessor = new CustomDBObjectAccessor(dbo); //NBA : all but this line is c&p

		TypeInformation<?> valueType = ClassTypeInformation.from(obj.getClass());
		TypeInformation<?> type = prop.getTypeInformation();

		if (valueType.isCollectionLike()) {
			DBObject collectionInternal = createCollection(asCollection(obj), prop);
			accessor.put(prop, collectionInternal);
			return;
		}

		if (valueType.isMap()) {
			DBObject mapDbObj = createMap((Map<Object, Object>) obj, prop);
			accessor.put(prop, mapDbObj);
			return;
		}

		if (prop.isDbReference()) {

			DBRef dbRefObj = null;

			/*
			 * If we already have a LazyLoadingProxy, we use it's cached DBRef value instead of 
			 * unnecessarily initializing it only to convert it to a DBRef a few instructions later.
			 */
			if (obj instanceof LazyLoadingProxy) {
				dbRefObj = ((LazyLoadingProxy) obj).toDBRef();
			}

			dbRefObj = dbRefObj != null ? dbRefObj : createDBRef(obj, prop);

			if (null != dbRefObj) {
				accessor.put(prop, dbRefObj);
				return;
			}
		}

		/*
		 * If we have a LazyLoadingProxy we make sure it is initialized first.
		 */
		if (obj instanceof LazyLoadingProxy) {
			obj = ((LazyLoadingProxy) obj).getTarget();
		}

		// Lookup potential custom target type
		Class<?> basicTargetType = conversions.getCustomWriteTarget(obj.getClass(), null);

		if (basicTargetType != null) {
			accessor.put(prop, conversionService.convert(obj, basicTargetType));
			return;
		}

		Object existingValue = accessor.get(prop);
		BasicDBObject propDbObj = existingValue instanceof BasicDBObject ? (BasicDBObject) existingValue : new BasicDBObject();
		addCustomTypeKeyIfNecessary(ClassTypeInformation.from(prop.getRawType()), obj, propDbObj);

		MongoPersistentEntity<?> entity = isSubtype(prop.getType(), obj.getClass())
				? mappingContext.getPersistentEntity(obj.getClass()) : mappingContext.getPersistentEntity(type);

		writeInternal(obj, propDbObj, entity);
		accessor.put(prop, propDbObj);
	}

    private boolean isSubtype(Class<?> left, Class<?> right) {
		return left.isAssignableFrom(right) && !left.equals(right);
	}
    
    private static Collection<?> asCollection(Object source) {
		if (source instanceof Collection) {
			return (Collection<?>) source;
		}
		return source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singleton(source);
	}
    
    private void writeSimpleInternal(Object value, DBObject dbObject, MongoPersistentProperty property) {
		CustomDBObjectAccessor accessor = new CustomDBObjectAccessor(dbObject); //NBA : all but this line is c&p
		accessor.put(property, getPotentiallyConvertedSimpleWrite(value));
	}
    
	private Object getPotentiallyConvertedSimpleWrite(Object value) {
		if (value == null) {
			return null;
		}
		Class<?> customTarget = conversions.getCustomWriteTarget(value.getClass(), null);

		if (customTarget != null) {
			return conversionService.convert(value, customTarget);
		} else {
			return Enum.class.isAssignableFrom(value.getClass()) ? ((Enum<?>) value).name() : value;
		}
	}

}