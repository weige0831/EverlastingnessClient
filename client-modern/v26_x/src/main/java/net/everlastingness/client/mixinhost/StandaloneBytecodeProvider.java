package net.everlastingness.client.mixinhost;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;

/**
 * Minimal {@link IClassBytecodeProvider} that reads class bytecode from the
 * classpath and parses it into an ASM {@link ClassNode} for Mixin's transformer.
 *
 * <p>Mixin calls this when it needs the bytecode representation of a class it
 * is about to transform (e.g. to read annotations, fields, methods). The
 * {@code runTransformers} flag is ignored — there is no upstream transformer
 * pipeline in a standalone agent.</p>
 */
final class StandaloneBytecodeProvider implements IClassBytecodeProvider {

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, false, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers)
            throws ClassNotFoundException, IOException {
        return getClassNode(name, runTransformers, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags)
            throws ClassNotFoundException, IOException {
        String resource = name.replace('.', '/') + ".class";
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = StandaloneBytecodeProvider.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(resource)) {
            if (in == null) {
                throw new ClassNotFoundException(name);
            }
            ClassReader reader = new ClassReader(in);
            ClassNode node = new ClassNode();
            reader.accept(node, readerFlags);
            return node;
        }
    }
}
