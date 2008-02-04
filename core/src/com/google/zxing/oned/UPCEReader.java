/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.oned;

import com.google.zxing.ReaderException;
import com.google.zxing.common.BitArray;

/**
 * <p>Implements decoding of the UPC-E format.</p>
 * <p/>
 * <p><a href="http://www.barcodeisland.com/upce.phtml">This</a> is a great reference for
 * UPC-E information.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class UPCEReader extends AbstractUPCEANReader {

  /**
   * The pattern that marks the middle, and end, of a UPC-E pattern.
   * There is no "second half" to a UPC-E barcode.
   */
  private static final int[] MIDDLE_END_PATTERN = {1, 1, 1, 1, 1, 1};

  /**
   * See {@link #L_AND_G_PATTERNS}; these values similarly represent patterns of
   * even-odd parity encodings of digits that imply both the number system (0 or 1)
   * used, and the check digit.
   */
  private static final int[][] NUMSYS_AND_CHECK_DIGIT_PATTERNS = {
      {0x38, 0x34, 0x32, 0x31, 0x2C, 0x26, 0x23, 0x2A, 0x29, 0x25},
      {0x07, 0x0B, 0x0D, 0x0E, 0x13, 0x19, 0x1C, 0x15, 0x16, 0x1A}
  };

  protected int decodeMiddle(BitArray row, int[] startRange, StringBuffer result) throws ReaderException {

    int[] counters = new int[4];
    int end = row.getSize();
    int rowOffset = startRange[1];

    int lgPatternFound = 0;

    for (int x = 0; x < 6 && rowOffset < end; x++) {
      int bestMatch = decodeDigit(row, counters, rowOffset, L_AND_G_PATTERNS);
      result.append((char) ('0' + bestMatch % 10));
      for (int i = 0; i < counters.length; i++) {
        rowOffset += counters[i];
      }
      if (bestMatch >= 10) {
        lgPatternFound |= 1 << (5 - x);
      }
    }

    determineNumSysAndCheckDigit(result, lgPatternFound);

    return rowOffset;
  }

  protected int[] decodeEnd(BitArray row, int endStart) throws ReaderException {
    return findGuardPattern(row, endStart, true, MIDDLE_END_PATTERN);
  }

  protected boolean checkChecksum(String s) throws ReaderException {
    return super.checkChecksum(convertUPCEtoUPCA(s));
  }

  private static void determineNumSysAndCheckDigit(StringBuffer resultString, int lgPatternFound)
      throws ReaderException {

    for (int numSys = 0; numSys <= 1; numSys++) {
      for (int d = 0; d < 10; d++) {
        if (lgPatternFound == NUMSYS_AND_CHECK_DIGIT_PATTERNS[numSys][d]) {
          resultString.insert(0, (char) ('0' + numSys));
          resultString.append((char) ('0' + d));
          return;
        }
      }
    }
    throw new ReaderException("Unable to determine number system and check digit");
  }

  /**
   * Expands a UPC-E value back into its full, equivalent UPC-A code value.
   *
   * @param upce UPC-E code as string of digits
   * @return equivalent UPC-A code as string of digits
   */
  private static String convertUPCEtoUPCA(String upce) {
    char[] upceChars = new char[6];
    upce.getChars(1, 7, upceChars, 0);
    StringBuffer result = new StringBuffer(12);
    result.append(upce.charAt(0));
    char lastChar = upceChars[5];
    switch (lastChar) {
      case '0':
      case '1':
      case '2':
        result.append(upceChars, 0, 2);
        result.append(lastChar);
        result.append("0000");
        result.append(upceChars, 2, 3);
        break;
      case '3':
        result.append(upceChars, 0, 3);
        result.append("00000");
        result.append(upceChars, 3, 2);
        break;
      case '4':
        result.append(upceChars, 0, 4);
        result.append("00000");
        result.append(upceChars[4]);
        break;
      default:
        result.append(upceChars, 0, 5);
        result.append("0000");
        result.append(lastChar);
        break;
    }
    result.append(upce.charAt(7));
    return result.toString();
  }

}