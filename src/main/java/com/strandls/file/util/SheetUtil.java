package com.strandls.file.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class SheetUtil {

	XSSFWorkbook workbook = null;
	String filePath = null;

	public SheetUtil(String filePath) {
		this.filePath = filePath;
	}

	private Map<String, Object> readExcelFile(String filePath) {
		try {
			FileInputStream excelFile = new FileInputStream(new File(filePath));
			XSSFWorkbook workbook = new XSSFWorkbook(excelFile);

			XSSFSheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rows = sheet.iterator();
			Map<String, Object> observationList = new HashMap<String, Object>();
			List<Object> rowData = new ArrayList<Object>();
			List<String> headerList = new ArrayList<String>();

			int rowNumber = 0;
			while (rows.hasNext() && rowNumber <= 3) {
				Row currentRow = rows.next();
				if (rowNumber == 0) {
					Iterator<Cell> headerRow = currentRow.iterator();
					headerList = extractHeaders(headerRow);
				} else {
					List<String> Hist = headerList;
					Map<String, Object> cust = new HashMap<String, Object>();
					int index = 0;
					for (String item : Hist) {
						Cell currentCell = currentRow.getCell(index);
						if (item.matches("observed_on")) {
							cust.put(item, currentCell != null ? currentCell.getLocalDateTimeCellValue().toString() : "");
						} else {
							cust.put(item, currentCell != null ? cellToObject(currentCell) : "");
						}
						index++;
					}
					rowData.add(cust);
				}
				rowNumber++;
			}
			// Close WorkBook
			workbook.close();
			observationList.put("rowData", rowData);
			observationList.put("headerData", headerList);
			return observationList;
		} catch (IOException e) {
			throw new RuntimeException("FAIL! -> message = " + e.getMessage());
		}
	}

	private List<String> extractHeaders(Iterator<Cell> cellsInRow) {
		List<String> headers = new ArrayList<String>();
		cellsInRow.forEachRemaining((Cell item) -> {
			if (!item.getStringCellValue().isEmpty()) {
				headers.add(cleanString(item.getStringCellValue()));
			}
		});
		return headers;
	}

	/**
	 * extract data from single cell
	 * 
	 * @param cell
	 * @return
	 */

	private Object cellToObject(Cell cell) {
		CellType type = cell.getCellType();
		switch (type) {
		case STRING:
			return cleanString(cell.getStringCellValue());
		case BOOLEAN:
			return cell.getBooleanCellValue();
		case NUMERIC:
			return cell.getNumericCellValue();
		case BLANK:
			return "";
		default:
			return "";
		}
	}

	/**
	 * Removes whitespace from the given string
	 * 
	 * @param string
	 * @return
	 */
	private String cleanString(String string) {
		return string.replace("\n", "").replace("\r", "");
	}

	/**
	 * Convert Java Objects to JSON String
	 * 
	 * @param customers
	 * @param fileName
	 */
	public Map<String, Object> convertObjects2JsonString() {
		return readExcelFile(filePath);
	}

}
