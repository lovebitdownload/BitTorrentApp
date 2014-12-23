/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.frostwire.jlibtorrent.swig;

public class lazy_entry {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected lazy_entry(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(lazy_entry obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        libtorrent_jni.delete_lazy_entry(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public lazy_entry() {
    this(libtorrent_jni.new_lazy_entry(), true);
  }

  public lazy_entry.entry_type_t type() {
    return lazy_entry.entry_type_t.swigToEnum(libtorrent_jni.lazy_entry_type(swigCPtr, this));
  }

  public void construct_int(String start, int length) {
    libtorrent_jni.lazy_entry_construct_int(swigCPtr, this, start, length);
  }

  public long int_value() {
    return libtorrent_jni.lazy_entry_int_value(swigCPtr, this);
  }

  public void construct_string(String start, int length) {
    libtorrent_jni.lazy_entry_construct_string(swigCPtr, this, start, length);
  }

  public String string_ptr() {
    return libtorrent_jni.lazy_entry_string_ptr(swigCPtr, this);
  }

  public String string_cstr() {
    return libtorrent_jni.lazy_entry_string_cstr(swigCPtr, this);
  }

  public pascal_string string_pstr() {
    return new pascal_string(libtorrent_jni.lazy_entry_string_pstr(swigCPtr, this), true);
  }

  public String string_value() {
    return libtorrent_jni.lazy_entry_string_value(swigCPtr, this);
  }

  public int string_length() {
    return libtorrent_jni.lazy_entry_string_length(swigCPtr, this);
  }

  public void construct_dict(String begin) {
    libtorrent_jni.lazy_entry_construct_dict(swigCPtr, this, begin);
  }

  public lazy_entry dict_append(String name) {
    long cPtr = libtorrent_jni.lazy_entry_dict_append(swigCPtr, this, name);
    return (cPtr == 0) ? null : new lazy_entry(cPtr, false);
  }

  public void pop() {
    libtorrent_jni.lazy_entry_pop(swigCPtr, this);
  }

  public lazy_entry dict_find(String name) {
    long cPtr = libtorrent_jni.lazy_entry_dict_find(swigCPtr, this, name);
    return (cPtr == 0) ? null : new lazy_entry(cPtr, false);
  }

  public lazy_entry dict_find_string(String name) {
    long cPtr = libtorrent_jni.lazy_entry_dict_find_string(swigCPtr, this, name);
    return (cPtr == 0) ? null : new lazy_entry(cPtr, false);
  }

  public String dict_find_string_value(String name) {
    return libtorrent_jni.lazy_entry_dict_find_string_value(swigCPtr, this, name);
  }

  public pascal_string dict_find_pstr(String name) {
    return new pascal_string(libtorrent_jni.lazy_entry_dict_find_pstr(swigCPtr, this, name), true);
  }

  public long dict_find_int_value(String name, long default_val) {
    return libtorrent_jni.lazy_entry_dict_find_int_value__SWIG_0(swigCPtr, this, name, default_val);
  }

  public long dict_find_int_value(String name) {
    return libtorrent_jni.lazy_entry_dict_find_int_value__SWIG_1(swigCPtr, this, name);
  }

  public lazy_entry dict_find_int(String name) {
    long cPtr = libtorrent_jni.lazy_entry_dict_find_int(swigCPtr, this, name);
    return (cPtr == 0) ? null : new lazy_entry(cPtr, false);
  }

  public lazy_entry dict_find_dict(String name) {
    long cPtr = libtorrent_jni.lazy_entry_dict_find_dict(swigCPtr, this, name);
    return (cPtr == 0) ? null : new lazy_entry(cPtr, false);
  }

  public lazy_entry dict_find_list(String name) {
    long cPtr = libtorrent_jni.lazy_entry_dict_find_list(swigCPtr, this, name);
    return (cPtr == 0) ? null : new lazy_entry(cPtr, false);
  }

  public string_lazy_entry_const_ptr_pair dict_at(int i) {
    return new string_lazy_entry_const_ptr_pair(libtorrent_jni.lazy_entry_dict_at(swigCPtr, this, i), true);
  }

  public int dict_size() {
    return libtorrent_jni.lazy_entry_dict_size(swigCPtr, this);
  }

  public void construct_list(String begin) {
    libtorrent_jni.lazy_entry_construct_list(swigCPtr, this, begin);
  }

  public lazy_entry list_append() {
    long cPtr = libtorrent_jni.lazy_entry_list_append(swigCPtr, this);
    return (cPtr == 0) ? null : new lazy_entry(cPtr, false);
  }

  public lazy_entry list_at(int i) {
    long cPtr = libtorrent_jni.lazy_entry_list_at(swigCPtr, this, i);
    return (cPtr == 0) ? null : new lazy_entry(cPtr, false);
  }

  public String list_string_value_at(int i) {
    return libtorrent_jni.lazy_entry_list_string_value_at(swigCPtr, this, i);
  }

  public pascal_string list_pstr_at(int i) {
    return new pascal_string(libtorrent_jni.lazy_entry_list_pstr_at(swigCPtr, this, i), true);
  }

  public long list_int_value_at(int i, long default_val) {
    return libtorrent_jni.lazy_entry_list_int_value_at__SWIG_0(swigCPtr, this, i, default_val);
  }

  public long list_int_value_at(int i) {
    return libtorrent_jni.lazy_entry_list_int_value_at__SWIG_1(swigCPtr, this, i);
  }

  public int list_size() {
    return libtorrent_jni.lazy_entry_list_size(swigCPtr, this);
  }

  public void set_end(String end) {
    libtorrent_jni.lazy_entry_set_end(swigCPtr, this, end);
  }

  public void clear() {
    libtorrent_jni.lazy_entry_clear(swigCPtr, this);
  }

  public void release() {
    libtorrent_jni.lazy_entry_release(swigCPtr, this);
  }

  public char_const_ptr_int_pair data_section() {
    return new char_const_ptr_int_pair(libtorrent_jni.lazy_entry_data_section(swigCPtr, this), true);
  }

  public void swap(lazy_entry e) {
    libtorrent_jni.lazy_entry_swap(swigCPtr, this, lazy_entry.getCPtr(e), e);
  }

  public static int bdecode(char_vector buffer, lazy_entry e, error_code ec) {
    return libtorrent_jni.lazy_entry_bdecode(char_vector.getCPtr(buffer), buffer, lazy_entry.getCPtr(e), e, error_code.getCPtr(ec), ec);
  }

  public enum entry_type_t {
    none_t,
    dict_t,
    list_t,
    string_t,
    int_t;

    public final int swigValue() {
      return swigValue;
    }

    public static entry_type_t swigToEnum(int swigValue) {
      entry_type_t[] swigValues = entry_type_t.class.getEnumConstants();
      if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
        return swigValues[swigValue];
      for (entry_type_t swigEnum : swigValues)
        if (swigEnum.swigValue == swigValue)
          return swigEnum;
      throw new IllegalArgumentException("No enum " + entry_type_t.class + " with value " + swigValue);
    }

    @SuppressWarnings("unused")
    private entry_type_t() {
      this.swigValue = SwigNext.next++;
    }

    @SuppressWarnings("unused")
    private entry_type_t(int swigValue) {
      this.swigValue = swigValue;
      SwigNext.next = swigValue+1;
    }

    @SuppressWarnings("unused")
    private entry_type_t(entry_type_t swigEnum) {
      this.swigValue = swigEnum.swigValue;
      SwigNext.next = this.swigValue+1;
    }

    private final int swigValue;

    private static class SwigNext {
      private static int next = 0;
    }
  }

}
