package org.positronicnet.tutorial.todo

import android.app.Activity
import android.os.Bundle

import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnKeyListener
import android.view.KeyEvent
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

import org.positronicnet.ui.IndexedSeqAdapter

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

class TodoItemsActivity extends Activity {

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)
    setContentView(R.layout.todo_items)

    TodoDb.openInContext( this )

    val adapter: IndexedSeqAdapter[ TodoItem ] = 
      new IndexedSeqAdapter(
        IndexedSeq.empty,
        itemViewResourceId = android.R.layout.simple_list_item_1 )
  
    val listView = findViewById( R.id.listItemsView ).asInstanceOf[ ListView ]
    val textView = findViewById( R.id.newItemText ).asInstanceOf[TextView]
    val button   = findViewById( R.id.addButton ).asInstanceOf[ Button ]

    listView.setAdapter( adapter )

    TodoItems ! Fetch{ adapter.resetSeq( _ ) }

    listView.setOnItemClickListener {
      new OnItemClickListener {
        override def onItemClick( parent: AdapterView[_], view: View, 
                                  posn: Int, id: Long ) = {
          TodoItems ! Delete( adapter.getItem( posn ) )
          TodoItems ! Fetch{ adapter.resetSeq( _ ) }
        }
      }
    }

    button.setOnClickListener{
      new OnClickListener {
        override def onClick(v: View) = { addItem }
      }
    }

    textView.setOnKeyListener{
      new OnKeyListener {
        def onKey( v: View, keyCode: Int, ev: KeyEvent ): Boolean = { 
          if (keyCode == KeyEvent.KEYCODE_ENTER
              && ev.getAction == KeyEvent.ACTION_DOWN) 
          {
            addItem
            return true
          }
          return false
        } 
      }
    }

    def addItem = {
      val text = textView.getText.toString.trim
      if (text != "") {
        TodoItems ! Save( new TodoItem( text ))
        TodoItems ! Fetch{ adapter.resetSeq( _ ) }
      }
      textView.setText( "" )
    }
  }

  override def onDestroy = {
    super.onDestroy
    TodoDb.close
  }
}
