package org.positronicnet.tutorial.todo

import android.app.Activity
import android.os.Bundle

import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.Button
import android.widget.TextView

import org.positronicnet.db.Database
import org.positronicnet.orm.RecordManager
import org.positronicnet.orm.ManagedRecord
import org.positronicnet.orm.Actions._
import org.positronicnet.notifications.Actions._

import org.positronicnet.ui.PositronicActivity
import org.positronicnet.ui.IndexedSeqSourceAdapter

object TodoDb extends Database( filename = "todos.sqlite3" ) 
{
  def schemaUpdates =
    List(""" create table todo_items (
               _id integer primary key,
               description string
             )
         """)
}

case class TodoItem( description: String = null, 
                     id: Long            = ManagedRecord.unsavedId 
                   )
  extends ManagedRecord( TodoItems )
{
  def setDescription( s: String ) = this.copy( description = s )
  override def toString = this.description
}

object TodoItems extends RecordManager[ TodoItem ]( TodoDb( "todo_items" ))

class TodoItemsActivity 
  extends PositronicActivity( layoutResourceId = R.layout.todo_items )
{
  def findView[T]( tr: TypedResource[T] )=findViewById( tr.id ).asInstanceOf[T]

  lazy val adapter: IndexedSeqSourceAdapter[ TodoItem ] = 
    new IndexedSeqSourceAdapter(
      this, TodoItems.records,
      itemViewResourceId = android.R.layout.simple_list_item_1 )
  
  onCreate {
    useAppFacility( TodoDb )
    findView( TR.listItemsView ).setAdapter( adapter )

    findView( TR.addButton ).onClick {
      val text = findView( TR.newItemText ).getText.toString.trim
      if (text != "") {
        TodoItems ! Save( new TodoItem( text ))
        findView( TR.newItemText ).setText( "" )
      }
    }

    findView( TR.listItemsView ).onItemClick{ (view, posn, id) =>
      TodoItems ! Delete( adapter.getItem( posn ))
    }
  }
}
