/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 *
 * @author jllach
 */
 public class One {
      @Id 
      String id;
      @Field("DATA.TITLE")
      String title;
      @Field("DATA.DESC")
      String desc;
      
        @PersistenceConstructor
        public One(String id, String title, String desc) {
            this.id = id;
            this.title = title;
            this.desc = desc;
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
}
