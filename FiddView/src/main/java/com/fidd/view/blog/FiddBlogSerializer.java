package com.fidd.view.blog;

import com.fidd.core.NamedEntry;

public interface FiddBlogSerializer extends NamedEntry {
    byte[] serialize(FiddBlog fiddKey);
    FiddBlog deserialize(byte[] fiddKeyBytes);
}
