package brix.jcr.base.wrapper;

import brix.jcr.base.action.AbstractActionHandler;

class BaseWrapper<T>
{

	private final T delegate;
	private final SessionWrapper session;

	public BaseWrapper(T delegate, SessionWrapper session)
	{
		this.delegate = delegate;
		this.session = session;
	}

	public T getDelegate()
	{
		return delegate;
	}

	public SessionWrapper getSessionWrapper()
	{
		return session;
	}

	Integer hashCode;

	/*
	 * Jackrabbit doesn't seem to implement either hashCode or equals, so we just rely on object identity 
	 
	@Override
	public int hashCode()
	{
		if (hashCode == null)
		{
			hashCode = getDelegate().hashCode();
		}
		return hashCode();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj instanceof BaseWrapper)
		{
			return false;
		}
		BaseWrapper wrapper = (BaseWrapper) obj;

		// optimization - check for hash code (if we know it)
		if (hashCode != null && wrapper.hashCode != null && hashCode != wrapper.hashCode())
		{
			return false;
		}
		else
		{
			return getDelegate() == wrapper.getDelegate() || getDelegate().equals(wrapper.getDelegate());
		}
	}
	*/

	@SuppressWarnings("unchecked")
	protected <TYPE> TYPE unwrap(TYPE wrapper)
	{
		while (wrapper instanceof BaseWrapper)
		{
			wrapper = (TYPE) ((BaseWrapper) wrapper).getDelegate();
		}
		return wrapper;
	}

	public <TYPE> TYPE[] unwrap(TYPE original[], TYPE newArray[])
	{
		for (int i = 0; i < original.length; ++i)
		{
			newArray[i] = unwrap(original[i]);
		}
		return newArray;
	}

	public AbstractActionHandler getActionHandler()
	{
		return getSessionWrapper().getActionHandler();
	}
}