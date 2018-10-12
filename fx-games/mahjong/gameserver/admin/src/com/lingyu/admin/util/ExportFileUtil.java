package com.lingyu.admin.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExportFileUtil {

	
	public final static void exportHssfFile(HttpServletResponse resp,String fileName,String[] columnName,List<Object[]> objList){
		resp.setContentType("application/octet-stream;charset=UTF-8");
		resp.setHeader("Content-disposition", "attachment; filename="+fileName);
		XSSFWorkbook book = new XSSFWorkbook();
		XSSFSheet sheet = book.createSheet();
		XSSFRow head = sheet.createRow(0);
		for(int i=0;i<columnName.length;i++){
			head.createCell(i).setCellValue(columnName[i]);
		}
		for(int i=0;i<objList.size();i++){
			XSSFRow row = sheet.createRow(i + 1);
			Object[] objArray = objList.get(i);
			for(int j=0;j<objArray.length;j++){
				 XSSFCell cell = row.createCell(j);
				 cell.setCellType(HSSFCell.CELL_TYPE_STRING);
				 if(objArray[j]!=null){
					 cell.setCellValue(objArray[j].toString());
				 }
			}
		}
		BufferedOutputStream out;
		try {
			out = new BufferedOutputStream(resp.getOutputStream());
		book.write(out);
		out.flush();
		out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public final static void exportXssfFile(HttpServletResponse resp,String[] columnName,List<Object[]> objList){
		exportHssfFile(resp,"export.xlsx",columnName,objList);
	}
}
