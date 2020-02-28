/**
 * Copyright (c) 2016, Miklos Maroti, University of Szeged
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

#include <jni.h>
#include <stdint.h>

#include <minisat/simp/SimpSolver.h>
#include "com_github_lipen_jnisat_JMiniSat.h"

#define JNI_METHOD(rtype, name) \
    JNIEXPORT rtype JNICALL Java_com_github_lipen_jnisat_JMiniSat_##name

static inline jlong encode(Minisat::SimpSolver* p) {
  return (jlong) (intptr_t) p;
}

static inline Minisat::SimpSolver* decode(jlong h) {
  return (Minisat::SimpSolver*) (intptr_t) h;
}

JNI_METHOD(jlong, minisat_1ctor)
  (JNIEnv*, jobject) {
    return encode(new Minisat::SimpSolver());
  }

JNI_METHOD(void, minisat_1dtor)
  (JNIEnv*, jobject, jlong handle) {
    delete decode(handle);
  }

JNI_METHOD(jint, minisat_1nvars)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->nVars();
}

JNI_METHOD(jint, minisat_1nclauses)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->nClauses();
}

JNI_METHOD(jint, minisat_1nlearnts)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->nLearnts();
}

JNI_METHOD(jint, minisat_1new_1var)
  (JNIEnv*, jobject, jlong handle, jbyte polarity, jboolean decision) {
    int v = decode(handle)->newVar(Minisat::lbool((uint8_t) polarity), decision);
    return (jint)(1 + v);
}

JNI_METHOD(void, minisat_1set_1polarity)
  (JNIEnv*, jobject, jlong handle, jint lit, jbyte polarity) {
    int v = lit > 0 ? lit - 1 : -lit - 1;
    decode(handle)->setPolarity(v, Minisat::lbool((uint8_t) polarity));
  }

JNI_METHOD(void, minisat_1set_1decision_1var)
  (JNIEnv*, jobject, jlong handle, jint lit, jboolean b) {
    int v = lit > 0 ? lit - 1 : -lit - 1;
    decode(handle)->setDecisionVar(v, b);
  }

JNI_METHOD(void, minisat_1set_1frozen)
  (JNIEnv*, jobject, jlong handle, jint lit, jboolean b) {
    int v = lit > 0 ? lit - 1 : -lit - 1;
    decode(handle)->setFrozen(v, b);
  }

JNI_METHOD(void, minisat_1freeze)
  (JNIEnv*, jobject, jlong handle, jint lit) {
    int v = lit > 0 ? lit - 1 : -lit - 1;
    decode(handle)->freezeVar(v);
  }

JNI_METHOD(void, minisat_1thaw)
  (JNIEnv*, jobject, jlong handle) {
    decode(handle)->thaw();
  }

static inline Minisat::Lit convert(int lit) {
  return Minisat::toLit(lit > 0 ? (lit << 1) - 2 : ((-lit) << 1) - 1);
}

JNI_METHOD(jboolean, minisat_1add_1clause__JI)
  (JNIEnv*, jobject, jlong handle, jint lit) {
    return decode(handle)->addClause(convert(lit));
  }

JNI_METHOD(jboolean, minisat_1add_1clause__JII)
  (JNIEnv*, jobject, jlong handle, jint lit1, jint lit2) {
    return decode(handle)->addClause(convert(lit1), convert(lit2));
  }

JNI_METHOD(jboolean, minisat_1add_1clause__JIII)
  (JNIEnv*, jobject, jlong handle, jint lit1, jint lit2, jint lit3) {
    return decode(handle)->addClause(convert(lit1), convert(lit2), convert(lit3));
  }

JNI_METHOD(jboolean, minisat_1add_1clause__J_3I)
  (JNIEnv* env, jobject, jlong handle, jintArray lits) {
    jint len = env->GetArrayLength(lits);
    Minisat::vec<Minisat::Lit> vec(len);

    jint* p = (jint*) env->GetPrimitiveArrayCritical(lits, 0);
    for (jint i = 0; i < len; i++) {
        vec[i] = convert(p[i]);
    }
    env->ReleasePrimitiveArrayCritical(lits, p, 0);

    return decode(handle)->addClause_(vec);
  }

JNI_METHOD(jboolean, minisat_1solve__JZZ)
  (JNIEnv*, jobject, jlong handle, jboolean do_simp, jboolean turn_off_simp) {
    return decode(handle)->solve((bool) do_simp, (bool) turn_off_simp);
  }

JNI_METHOD(jboolean, minisat_1solve__JIZZ)
  (JNIEnv*, jobject, jlong handle, jint p, jboolean do_simp, jboolean turn_off_simp) {
    return decode(handle)->solve(convert(p), (bool) do_simp, (bool) turn_off_simp);
  }

JNI_METHOD(jboolean, minisat_1solve__JIIZZ)
  (JNIEnv*, jobject, jlong handle, jint p, jint q, jboolean do_simp, jboolean turn_off_simp) {
    return decode(handle)->solve(convert(p), convert(q), (bool) do_simp, (bool) turn_off_simp);
  }

JNI_METHOD(jboolean, minisat_1solve__JIIIZZ)
  (JNIEnv*, jobject, jlong handle, jint p, jint q, jint r, jboolean do_simp, jboolean turn_off_simp) {
    return decode(handle)->solve(convert(p), convert(q), convert(r), (bool) do_simp, (bool) turn_off_simp);
  }

JNI_METHOD(jboolean, minisat_1solve__J_3IZZ)
  (JNIEnv* env, jobject, jlong handle, jintArray assumptions, jboolean do_simp, jboolean turn_off_simp) {
    jint len = env->GetArrayLength(assumptions);
    Minisat::vec<Minisat::Lit> vec(len);

    jint* p = (jint*) env->GetPrimitiveArrayCritical(assumptions, 0);
    for (jint i = 0; i < len; i++) {
        vec[i] = convert(p[i]);
    }
    env->ReleasePrimitiveArrayCritical(assumptions, p, 0);

    return decode(handle)->solve(vec, (bool) do_simp, (bool) turn_off_simp);
  }

JNI_METHOD(jboolean, minisat_1simplify)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->simplify();
  }

JNI_METHOD(jboolean, minisat_1eliminate)
  (JNIEnv*, jobject, jlong handle, jboolean turn_off_simp) {
    return decode(handle)->eliminate((bool) turn_off_simp);
  }

JNI_METHOD(jboolean, minisat_1is_1eliminated)
  (JNIEnv*, jobject, jlong handle, jint lit) {
    int v = lit > 0 ? lit - 1 : -lit - 1;
    return decode(handle)->isEliminated(v);
  }

JNI_METHOD(jboolean, minisat_1okay)
  (JNIEnv*, jobject, jlong handle) {
    return decode(handle)->okay();
  }

JNI_METHOD(jbyte, minisat_1model_1value)
  (JNIEnv*, jobject, jlong handle, jint lit) {
    return (jbyte) Minisat::toInt(decode(handle)->modelValue(convert(lit)));
  }

JNI_METHOD(jbooleanArray, minisat_1get_1model)
  (JNIEnv* env, jobject, jlong handle) {
    Minisat::SimpSolver* solver = decode(handle);
    int size = solver->nVars() + 1;
    jbooleanArray result = env->NewBooleanArray(size);
    if (result == NULL) {
        return NULL;
    }
    jboolean* model = new jboolean[size];
    for (int i = 1; i < size; i++) {
        model[i] = solver->modelValue(convert(i)) == Minisat::l_True;
    }
    env->SetBooleanArrayRegion(result, 0, size, model);
    delete[] model;
    return result;
}
