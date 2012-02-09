package org.positronicnet.tutorial.todo

import android.app.Activity
import android.os.Bundle

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.KeyEvent
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.Button
import android.widget.TextView
import android.graphics.Paint

import org.positronicnet.db.Database
import org.positronicnet.orm.RecordManager
import org.positronicnet.orm.ManagedRecord
import org.positronicnet.orm.RecordId
import org.positronicnet.orm.Actions._
import org.positronicnet.notifications.Actions._

import org.positronicnet.ui.PositronicActivityHelpers
import org.positronicnet.ui.IndexedSeqSourceAdapter

object TodoDb extends Database( filename = "todos.sqlite3" ) 
{
  def schemaUpdates =
    List(""" create table todo_items (
               _id integer primary key,
               description string
             )
         """,
         """ alter table todo_items add column is_done integer default 0 """
       )
}

case class TodoItem( description: String             = null, 
                     isDone:      Boolean            = false,
                     id:          RecordId[TodoItem] = TodoItem.unsavedId
                   )
  extends ManagedRecord
{
  def setDescription( s: String ) = this.copy( description = s )
  def toggleDone = this.copy( isDone = !isDone )
  override def toString = this.description
}

object TodoItem extends RecordManager[ TodoItem ]( TodoDb( "todo_items" ))

class TodoItemsAdapter( activity: PositronicActivityHelpers )
  extends IndexedSeqSourceAdapter[ TodoItem ]( 
    activity, TodoItem, itemViewResourceId = R.layout.todo_item )
{
  override def bindView( view: View, it: TodoItem ) =
    view.asInstanceOf[ TodoItemView ].setTodoItem( it )
}

class TodoItemView( context: Context, attrs: AttributeSet = null )
 extends TextView( context, attrs ) 
{
   def setTodoItem( item: TodoItem ) = {
     setText( item.description )
     setPaintFlags( 
       if (item.isDone) getPaintFlags | Paint.STRIKE_THRU_TEXT_FLAG 
       else getPaintFlags & ~Paint.STRIKE_THRU_TEXT_FLAG
     )
   }
}

class TodoItemsActivity 
  extends Activity with PositronicActivityHelpers with TypedViewHolder
{
  onCreate {
    setContentView( R.layout.todo_items )
    useAppFacility( TodoDb )
    useOptionsMenuResource( R.menu.todo_items_menu )

    val adapter = new TodoItemsAdapter( this )
    findView( TR.listItemsView ).setAdapter( adapter )

    findView( TR.listItemsView ).onItemClick{ (view, posn, id) =>
      TodoItem ! Save( adapter.getItem( posn ).toggleDone )
    }
    findView( TR.addButton ).onClick { addItem }
    findView( TR.newItemText ).onKey( KeyEvent.KEYCODE_ENTER ){ addItem }

    onOptionsItemSelected( R.id.delete_where_done ) { 
      TodoItem.whereEq( "isDone" -> true ) ! DeleteAll
    }

    def addItem = {
      val text = findView( TR.newItemText ).getText.toString.trim
      if (text != "") {
        TodoItem ! Save( new TodoItem( text ))
        findView( TR.newItemText ).setText( "" )
      }
    }
  }
}
