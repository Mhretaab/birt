/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.engine.executor;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.engine.api.InstanceID;
import org.eclipse.birt.report.engine.content.IContent;
import org.eclipse.birt.report.engine.extension.IBaseResultSet;
import org.eclipse.birt.report.engine.extension.IReportItemExecutor;
import org.eclipse.birt.report.engine.ir.ReportItemDesign;

/**
 * It is a wrapper of user created executor.
 */
public class ExtendedItemExecutor extends ReportItemExecutor
{

	/**
	 * the user created executor.
	 */
	protected IReportItemExecutor executor;

	/**
	 * @param context
	 *            the engine execution context
	 * @param visitor
	 *            visitor class used to visit the extended item
	 */
	public ExtendedItemExecutor( ExecutorManager manager )
	{
		super( manager, ExecutorManager.EXTENDEDITEM );
	}

	public IContent execute( ) throws BirtException
	{
		// create user-defined generation-time helper object

		if ( executor != null )
		{
			// initialize the parent result sets.
			getParentResultSet( );
			
			// user implement the IReportItemExecutor.
			if ( executor instanceof ExtendedGenerateExecutor )
			{
				ExtendedGenerateExecutor gExecutor = (ExtendedGenerateExecutor) executor;
				gExecutor.parent = parent;
				gExecutor.context = context;
				gExecutor.report = report;
				gExecutor.design = design;
			}

			content = executor.execute( );

			if ( content != null )
			{
				IContent pContent = (IContent) content.getParent( );
				if ( pContent == null )
				{
					pContent = getParentContent( );
					content.setParent( pContent );
				}

				InstanceID iid = content.getInstanceID( );
				if ( iid != null )
				{
					InstanceID pid = iid.getParentID( );
					if ( pid == null && pContent != null )
					{
						pid = pContent.getInstanceID( );
					}
					long uid = iid.getUniqueID( );
					if ( uid == -1 )
					{
						uid = generateUniqueID( );
					}
					iid = new InstanceID( pid, uid, iid.getComponentID( ), iid
							.getDataID( ) );
					content.setInstanceID( iid );
					this.instanceId = iid;
				}
				else
				{
					iid = getInstanceID( );
					content.setInstanceID( iid );
				}
				
				//setup generate by
				if (content.getGenerateBy( ) == null)
				{
					if (design == null)
					{
						//find the design through the instanceid's component
						long componentId = content.getInstanceID( ).getComponentID( );
						if (componentId != -1)
						{
							design = (ReportItemDesign)report.getDesign( ).getReportItemByID( componentId );
						}
					}
					content.setGenerateBy( design );
				}

				if ( context.isInFactory( ) )
				{
					// context.execute( design.getOnCreate( ) );
					handleOnCreate( content );
				}

				setupBookmark( );
				startTOCEntry( content );
			}
		}
		return content;
	}

	private void setupBookmark( )
	{
		if ( content.getBookmark( ) == null )
		{
			if ( design != null && design.getQuery( ) != null )
			{
				content.setBookmark( manager.nextBookmarkID( ) );
			}
		}
	}

	public boolean hasNextChild( ) throws BirtException
	{
		if ( executor != null )
		{
			return executor.hasNextChild( );
		}
		return false;
	}

	public IReportItemExecutor getNextChild( ) throws BirtException
	{
		if ( executor != null )
		{
			IReportItemExecutor child = executor.getNextChild( );
			/*
			 *  this child executor could be:
			 *  1. system executor
			 *  2. extended item executor
			 *  3. user executor
			 *  
			 */
			if ( child != null )
			{
				// the child is a system element, the parent should be set to
				// the
				// wrapper.
				if ( child instanceof ReportItemExecutor )
				{
					child.setParent( this );
					return child;
				}
				// the child is an extended element, create a wrapper of that
				// executor.
				// the wrapper's parent is the parent wrapper.
				return manager.createExtendedExecutor( this, child );
			}
		}
		return null;
	}

	public void close( ) throws BirtException
	{
		if ( executor != null )
		{
			executor.close( );
			finishTOCEntry( );
			
			// restore the parent result set to context.
			context.setResultSets( parentRsets );
		}
		executor = null;
		super.close( );
	}

	public IBaseResultSet[] getQueryResults( )
	{
		if ( executor != null )
		{
			return executor.getQueryResults( );
		}
		return null;
	}
	
	public void setParent( IReportItemExecutor parent )
	{
		if ( executor != null )
		{
			if ( executor.getParent( ) == null )
			{
				IReportItemExecutor tmpExecutor = parent;
				if ( !( executor instanceof ReportItemExecutor ) )
				{
					if ( tmpExecutor instanceof ExtendedItemExecutor )
					{
						tmpExecutor = ( (ExtendedItemExecutor) tmpExecutor ).executor;
					}
				}
				executor.setParent( tmpExecutor );
			}
		}
		super.setParent( parent );
	}	
	
}