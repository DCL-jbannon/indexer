package org.API.OverDrive;

import java.util.*;

import org.json.simple.JSONObject;

@SuppressWarnings("rawtypes")
public class OverDriveCollectionIterator implements IOverDriveCollectionIterator, Iterator
{
		private Integer limit = 300;
		private Integer offset = 0;
		private Integer totalItems = -1;
		private IOverDriveAPIServices odas;
	
		public OverDriveCollectionIterator(String clientKey, String clientSecret, int libraryId)
		{
			this(new OverDriveAPIServices(clientKey, clientSecret, libraryId));
		}
		
		public OverDriveCollectionIterator(IOverDriveAPIServices overDriveAPIServices)
		{
			this.odas = overDriveAPIServices;
		}
		
		private void getTotalItems()
		{
			JSONObject result = this.odas.getDigitalCollection(1, 0);//Let's get the total items that the collection has.
			this.totalItems = ((Long)result.get("totalItems")).intValue();
		}
		
		@Override
		public JSONObject next()
		{	
			JSONObject result = this.odas.getDigitalCollection(this.limit, this.offset);
			this.offset += this.limit;
			return result;
		}
		
		@Override
		public boolean hasNext() 
		{
			if(this.totalItems == -1)
			{
				this.getTotalItems();
			}
			if(this.totalItems > this.offset)
			{
				return true;
			}
			return false;
		}
		
		@Override
		public void remove(){}
		
		/**
		 * Test purpouse
		 * @param int totalItems
		 */
		public void setTotalItems(int totalItems)
		{
			this.totalItems = totalItems;
		}
}
