/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo;

import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 *
 * @author jllach
 */
 public class Three {

      @Id 
      String id;
      
      @Field("DATA")
      Map    map;
      
      @Field("DATA.TITLE") 
      String title;
      @Field("DATA.DESC")
      String desc;
      
      
        @PersistenceConstructor
        public Three(String id, String title, String desc, Map map) {
            this.id    = id;
            this.title = title;
            this.desc  = desc;
            this.map   = map;
        }
        public String getId() {
            return id;
        }
        public String getTitle() {
            return title;
        }
        public String getDesc() {
            return desc;
        }
        public Map getMap() {
            return map;
        }
}
