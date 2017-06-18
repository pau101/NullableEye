package com.pau101.nullableeye.inspector;

import com.pau101.nullableeye.Nullity;
import com.pau101.nullableeye.inspection.Discoverer;
import com.pau101.nullableeye.inspection.Inspection;
import com.pau101.nullableeye.inspection.InspectionType;
import com.pau101.nullableeye.inspection.location.FieldLocation;
import com.pau101.nullableeye.inspection.location.Location;
import com.pau101.nullableeye.inspection.location.MethodLocation;
import com.pau101.nullableeye.inspection.location.ParameterLocation;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;
import java.util.function.Consumer;

public final class InspectorInterpreter extends BasicInterpreter {
	private final Inspector inspector;

	private MethodLocation location;

	private InspectorInterpreter(Inspector inspector) {
		this.inspector = inspector;
	}

	public void setLocation(MethodLocation location) {
		this.location = location;
	}

	@Override
	public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
		BasicValue valueOut = super.unaryOperation(insn, value);
		if (insn.getOpcode() == IFNULL || insn.getOpcode() == IFNONNULL) {
			if (value instanceof AnnotatedValue<?>) {
				AnnotatedValue<?> annotatedValue = (AnnotatedValue<?>) value;
				if (annotatedValue.nullity == Nullity.NONNULL) {
					annotatedValue.inspect(Discoverer.STATIC, InspectionType.POSSIBLY_NULLABLE);
				}
			}
		} else if (insn.getOpcode() == GETFIELD || insn.getOpcode() == ARRAYLENGTH || insn.getOpcode() == ATHROW) {
			if (isNullable(value)) {
				recordUnsafeDereference((AnnotatedValue<?>) value);
			}
			if (insn.getOpcode() == GETFIELD && inspector.isSupplierInScope((FieldInsnNode) insn)) {
				FieldLocation field = inspector.getMappedField((FieldInsnNode) insn);
				valueOut = new AnnotatedValue<>(valueOut, inspector.getNullity(field), field, inspector::recordField);
			}
		}
		return valueOut;
	}

	@Override
	public BasicValue ternaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2, BasicValue value3) throws AnalyzerException {
		if ((insn.getOpcode() >= IASTORE && insn.getOpcode() <= SASTORE || insn.getOpcode() >= IALOAD && insn.getOpcode() <= SALOAD) && isNullable(value1)) {
			recordUnsafeDereference((AnnotatedValue<?>) value1);
		}
		return super.ternaryOperation(insn, value1, value2, value3);
	}

	@Override
	public BasicValue naryOperation(AbstractInsnNode insn, List<? extends BasicValue> values) throws AnalyzerException {
		BasicValue value = super.naryOperation(insn, values);
		if (value == BasicValue.REFERENCE_VALUE && insn.getType() == AbstractInsnNode.METHOD_INSN) {
			if (insn.getOpcode() != INVOKESTATIC) {
				BasicValue obj = values.get(0);
				if (isNullable(obj)) {
					recordUnsafeDereference((AnnotatedValue<?>) obj);
				}
			}
			MethodInsnNode invocation = (MethodInsnNode) insn;
			if (inspector.isSupplierInScope(invocation)) {
				MethodLocation loc = inspector.getMappedMethod(invocation);
				value = new AnnotatedValue<>(value, inspector.getNullity(loc), loc, inspector::recordMethod);
			}
		}
		return value;
	}

	private boolean isNullable(BasicValue value) {
		return value instanceof AnnotatedValue<?> && ((AnnotatedValue<?>) value).nullity == Nullity.NULLABLE;
	}

	private void recordUnsafeDereference(AnnotatedValue<?> value) {
		// TODO: Keep track of line number for unsafe dereference to be helpful
	}

	public static InspectorAnalyzer analyzer(Inspector inspector) {
		return new InspectorInterpreter(inspector).new InspectorAnalyzer();
	}

	private final class AnnotatedValue<T extends Location<T>> extends BasicValue {
		final Nullity nullity;

		final T source;

		final Consumer<Inspection<T>> recorder;

		AnnotatedValue(BasicValue value, Nullity nullity, T source, Consumer<Inspection<T>> recorder) {
			super(value.getType());
			this.nullity = nullity;
			this.source = source;
			this.recorder = recorder;
		}

		public void inspect(Discoverer discoverer, InspectionType inspectionType) {
			recorder.accept(new Inspection<>(discoverer, inspectionType, source));
		}
	}

	public final class InspectorAnalyzer extends Analyzer<BasicValue> {
		private InspectorAnalyzer() {
			super(InspectorInterpreter.this);
		}

		@Override
		protected void init(String owner, MethodNode m) throws AnalyzerException {
			setLocation(inspector.getMappedMethod(owner, m.name, m.desc));
			Frame<BasicValue>[] frames = getFrames();
			if (frames.length > 0) {
				Frame<BasicValue> frame = frames[0];
				Type[] args = Type.getArgumentTypes(m.desc);
				int local = (m.access & ACC_STATIC) == 0 ? 1 : 0;
				for (int i = 0; i < args.length; local += args[i++].getSize()) {
					ParameterLocation param = location.withParameter(i);
					Nullity nullity = inspector.getNullity(param);
					frame.setLocal(local, new AnnotatedValue<>(frame.getLocal(i), nullity, param, inspector::recordParameter));
				}
			}
		}
	}
}
