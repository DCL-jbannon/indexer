package org.dcl.utils;

import java.util.ArrayList;

public class ActiveEcontentUtils
{
	
    private static ArrayList<String> listActiveEcontent = new ArrayList<String>();

	public static void addActiveEcontent(String ilsId)
	{
		ActiveEcontentUtils.listActiveEcontent.add(ilsId);
	}
	
	public static ArrayList<String> getList()
	{
		return ActiveEcontentUtils.listActiveEcontent;
	}

	public static String getCommaSeparatedString()
	{
		String result = "";
		for (int i = 0; i < ActiveEcontentUtils.listActiveEcontent.size(); i++)
		{
			if(result.length() == 0)
			{
				result = ActiveEcontentUtils.listActiveEcontent.get(i);
			}
			else
			{
				result = result + "," + ActiveEcontentUtils.listActiveEcontent.get(i);
			}
		}
		return result;
	}	
}